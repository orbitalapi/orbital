package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import io.vyne.VersionedTypeReference
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskDetails
import io.vyne.cask.api.CaskIngestionErrorDto
import io.vyne.cask.api.CaskIngestionErrorDtoPage
import io.vyne.cask.api.ContentType
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.cask.api.JsonIngestionParameters
import io.vyne.cask.api.XmlIngestionParameters
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionErrorRepository
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.ingest.StreamSource
import io.vyne.cask.io.SplittableInputStream
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.query.CaskMessageSourceWriter
import io.vyne.cask.query.StoreCaskRawMessageRequest
import io.vyne.cask.websocket.CsvWebsocketRequest
import io.vyne.cask.websocket.JsonWebsocketRequest
import io.vyne.cask.websocket.XmlWebsocketRequest
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.VersionedType
import io.vyne.utils.batchTimed
import io.vyne.utils.log
import org.springframework.context.event.EventListener
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.util.*

@Component
class CaskService(
   private val schemaProvider: SchemaProvider,
   private val ingesterFactory: IngesterFactory,
   private val caskConfigRepository: CaskConfigRepository,
   private val caskDAO: CaskDAO,
   private val ingestionErrorRepository: IngestionErrorRepository,
   private val messageSourceWriter: CaskMessageSourceWriter = CaskMessageSourceWriter(caskDAO.largeObjectDataSource)
) {
   interface CaskServiceError {
      val message: String
   }

   @EventListener
   fun handleSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("SchemaSetChanged - invalidating resolved type cache")
      resolvedTypeCache.invalidateAll()
      resolvedTypeCache.cleanUp()
      cachingSchemaHolder.invalidateAll()
      cachingSchemaHolder.cleanUp()
      typeDbWrapperCache.invalidateAll()
      typeDbWrapperCache.cleanUp()
   }

   private val resolvedTypeCache = CacheBuilder.newBuilder()
      .build<String, Either<TypeError, VersionedType>>(object :
         CacheLoader<String, Either<TypeError, VersionedType>>() {
         override fun load(typeReference: String): Either<TypeError, VersionedType> {
            val schema = schemaProvider.schema()
            if (schema.types.isEmpty()) {
               log().warn("Empty schema, no types defined? Check the configuration please!")
               return Either.left(TypeError("Empty schema, no types defined."))
            }

            return try {
               // Type[], Type of lang.taxi.Array<OrderSummary>
               // schema.versionedType(lang.taxi.Array) throws error, investigate why
               val versionedTypeReference = VersionedTypeReference.parse(typeReference)
               Either.right(schema.versionedType(versionedTypeReference))
            } catch (e: Exception) {
               log().error("Type not found typeReference=${typeReference} errorMessage=${e.message}")
               Either.left(TypeError("Type reference '${typeReference}' not found."))
            }
         }
      })


   private val typeDbWrapperCache = CacheBuilder
      .newBuilder()
      .expireAfterAccess(Duration.ofSeconds(5))
      .build<TypeDbWrapperRequest, TypeDbWrapper>(object : CacheLoader<TypeDbWrapperRequest, TypeDbWrapper>() {
         override fun load(key: TypeDbWrapperRequest): TypeDbWrapper {
            return TypeDbWrapper(key.type, key.schema)
         }
      })

   private val cachingSchemaHolder = CacheBuilder
      .newBuilder()
      .expireAfterAccess(Duration.ofSeconds(5))
      .build<String, Schema>(object : CacheLoader<String, Schema>() {
         override fun load(key: String): Schema {
            log().info("Fetching schema from schemaProvider")
            return schemaProvider.schema()
         }
      })

   data class TypeError(override val message: String) : CaskServiceError
   data class ContentTypeError(override val message: String) : CaskServiceError

   val supportedContentTypes: List<ContentType> = listOf(ContentType.json, ContentType.csv)

   fun resolveType(typeReference: String): Either<TypeError, VersionedType> {
      return resolvedTypeCache.get(typeReference)
   }

   fun ingestRequest(
      request: CaskIngestionRequest,
      inputStream: InputStream,
      messageId: String = UUID.randomUUID().toString()
   ): Int {
      val schema = cachingSchemaHolder.get("foo")
      val versionedType = request.versionedType

      val messageSourceInputStream = SplittableInputStream.from(inputStream)
      val messagePayloadInputStream = messageSourceInputStream.split()

      messageSourceWriter.writeMessageSource(
         StoreCaskRawMessageRequest(
            messageId,
            versionedType,
            messageSourceInputStream,
            request.contentType,
            request.parameters
         )
      )
//         Flux.empty<InstanceAttributeSet>()
//
//
      val streamSource: StreamSource = batchTimed("build stream source") {
         request.buildStreamSource(
            input = messagePayloadInputStream,
            type = versionedType,
            schema = schema,
            messageId = messageId
         )
      }

      val dbWrapper = typeDbWrapperCache.get(TypeDbWrapperRequest(versionedType, schema))
      val ingestionStream = IngestionStream(
         versionedType,
         dbWrapper,
         streamSource
      )
//      return streamSource.records


      return batchTimed("IngesterFactory.ingest") {
         ingesterFactory
            .create(ingestionStream)
            .ingest()
      }
   }

   fun getCasks(): List<CaskConfig> {
      return caskConfigRepository.findAll()
   }

   fun getCaskDetails(tableName: String): CaskDetails {
      val count = caskDAO.countCaskRecords(tableName)
      val fullQualifiedName = caskConfigRepository.findByTableName(tableName)!!.qualifiedTypeName
      val now = Instant.now();
      val yesterday = now.minusSeconds(24 * 60 * 60);
      val ingestionErrorsCount = this.ingestionErrorRepository.countByFullyQualifiedNameAndInsertedAtBetween(
         fullQualifiedName,
         yesterday,
         now
      )
      return CaskDetails(count, ingestionErrorsCount)
   }

   fun deleteCask(tableName: String) {
      if (caskDAO.exists(tableName)) {
         log().info("deleting cask for table name $tableName")
         caskDAO.deleteCask(tableName)
      }
   }

   fun emptyCask(tableName: String) {
      if (caskDAO.exists(tableName)) {
         caskDAO.emptyCask(tableName)
      }
   }

   fun caskIngestionErrorsFor(
      tableName: String,
      page: Int,
      pageSize: Int,
      start: Instant,
      end: Instant
   ): CaskIngestionErrorDtoPage {
      // Please note that our query is based on type name not based on table name.
      return caskConfigRepository.findByTableName(tableName)?.let { caskConfig ->
         val result = ingestionErrorRepository
            .findByFullyQualifiedNameAndInsertedAtBetweenOrderByInsertedAtDesc(
               caskConfig.qualifiedTypeName,
               start,
               end,
               PageRequest.of(page, pageSize)
            )

         val items = result.content.map { ingestionError ->
            CaskIngestionErrorDto(
               caskMessageId = ingestionError.caskMessageId,
               createdAt = ingestionError.insertedAt,
               fqn = ingestionError.fullyQualifiedName,
               error = ingestionError.error
            )
         }
         CaskIngestionErrorDtoPage(
            items = items,
            currentPage = result.number.toLong(),
            totalItem = result.totalElements.toLong(),
            totalPages = result.totalPages.toLong()
         )
      } ?: CaskIngestionErrorDtoPage(listOf(), 0L, 0L, 0L)
   }

   fun caskIngestionMessage(caskMessageId: String): Pair<Resource, ContentType?> {
      return caskDAO.fetchRawCaskMessage(caskMessageId)?.let { (stream, contentType) ->
         ByteArrayResource(stream) to contentType
      } ?: InputStreamResource(ByteArrayInputStream(ByteArray(0))) to null
   }

   fun deleteCaskByTypeName(typeName: String) {
      log().info("Deleting cask for type => $typeName")
      caskConfigRepository
         .findAllByQualifiedTypeName(typeName)
         .forEach { caskConfig -> this.deleteCask(caskConfig.tableName) }
   }
}

interface CaskIngestionRequest {
   fun buildStreamSource(input: InputStream, type: VersionedType, schema: Schema, messageId: String): StreamSource

   val versionedType: VersionedType
   val contentType: ContentType

   // Emits the parameters that this ingestion request has used to be configured.
   // Will be persisted along with the message.
   val parameters: Any

   val debug: Boolean
   val nullValues: Set<String>

   companion object {
      fun fromContentTypeAndHeaders(
         contentType: ContentType,
         versionedType: VersionedType,
         mapper: ObjectMapper,
         queryParams: MultiValueMap<String, String?>,
         caskIngestionErrorProcessor: CaskIngestionErrorProcessor
      ): CaskIngestionRequest {
         return when (contentType) {
            ContentType.csv -> {
               val params = mapper.convertValue<CsvIngestionParameters>(queryParams.toMapOfListWhereMultiple())
               CsvWebsocketRequest(params, versionedType, caskIngestionErrorProcessor)
            }
            ContentType.json -> {
               val params = mapper.convertValue<JsonIngestionParameters>(queryParams.toMapOfListWhereMultiple())
               JsonWebsocketRequest(params, versionedType, mapper)
            }
            ContentType.xml -> {
               val parameters = mapper.convertValue<XmlIngestionParameters>(queryParams.toMapOfListWhereMultiple())
               XmlWebsocketRequest(parameters, versionedType)
            }
         }
      }
   }
}

private fun MultiValueMap<String, String?>.toMapOfListWhereMultiple(): Map<String, Any?> {
   return this.map { (key, value) ->
      key to when (value.size) {
         0 -> null
         1 -> value.first()
         else -> value
      }
   }.toMap()
}

private data class TypeDbWrapperRequest(val type: VersionedType, val schema: Schema)
