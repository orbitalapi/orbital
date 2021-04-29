package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.vyne.VersionedTypeReference
import io.vyne.cask.api.*
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionErrorRepository
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.websocket.CsvWebsocketRequest
import io.vyne.cask.websocket.JsonWebsocketRequest
import io.vyne.cask.websocket.XmlWebsocketRequest
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import reactor.core.publisher.Flux
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.util.*

@Component
class CaskService(private val schemaProvider: SchemaProvider,
                  private val ingesterFactory: IngesterFactory,
                  private val caskConfigRepository: CaskConfigRepository,
                  private val caskDAO: CaskDAO,
                  private val ingestionErrorRepository: IngestionErrorRepository,
                  private val caskViewService: CaskViewService) {

   interface CaskServiceError {
      val message: String
   }

   data class TypeError(override val message: String) : CaskServiceError
   data class ContentTypeError(override val message: String) : CaskServiceError

   val supportedContentTypes: List<ContentType> = listOf(ContentType.json, ContentType.csv)

   fun resolveType(typeReference: String): Either<TypeError, VersionedType> {
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

   fun ingestRequest(
      request: CaskIngestionRequest,
      input: Flux<InputStream>,
      messageId: String = UUID.randomUUID().toString()): Flux<InstanceAttributeSet> {
      val schema = schemaProvider.schema()
      val versionedType = request.versionedType
      // capturing path to the message
      val message = caskDAO.createCaskMessage(versionedType, messageId, input, request.contentType, request.parameters)
      val inputToProcess = if (message.messageContentId != null) {
         log().info("Message content for message ${message.id} was persisted, will stream from db")
         caskDAO.getMessageContent(message.messageContentId)
      } else {
         log().warn("Failed to persist message content for message ${message.id}.  Will continue to ingest, but this message will not be replayable")
         input
      }


      val streamSource: StreamSource = request.buildStreamSource(
         input = inputToProcess,
         type = versionedType,
         schema = schema,
         messageId = messageId
      )
      val ingestionStream = IngestionStream(
         versionedType,
         TypeDbWrapper(versionedType, schema),
         streamSource)

      return ingesterFactory
         .create(ingestionStream)
         .ingest()
   }

   fun getCasks(): List<CaskConfig> {
      return caskConfigRepository.findAll()
   }

   fun getCaskDetails(tableName: String): CaskDetails {
      val count = caskDAO.countCaskRecords(tableName)
      val caskConfig = caskConfigRepository.findByTableName(tableName)!!
      val fullQualifiedName = caskConfig.qualifiedTypeName
      val dependencies = if (caskConfig.exposesType) {
         emptyList()
      } else caskViewService.viewCaskDependencies(caskConfig).map { it.qualifiedTypeName }
      val now = Instant.now()
      val yesterday = now.minus(Duration.ofDays(1))
      val ingestionErrorsCount= this.ingestionErrorRepository.countByFullyQualifiedNameAndInsertedAtBetween(
         fullQualifiedName,
         yesterday,
         now
      )
      return CaskDetails(count, ingestionErrorsCount, dependencies)
   }

   /**
    * Deletes the given Cask configuration.
    * @param caskConfig Defines the cask that is to be deleted
    * @param force If there are other casks (view based) that depends on the cask to be deleted, these
    * dependent casks will also be deleted if force is true.
    */
   fun deleteCask(caskConfig: CaskConfig, force: Boolean = false) {
      log().info("deleting cask with config $caskConfig")
      val dependencies = if (!caskConfig.exposesType && force) {
         this.caskViewService.viewCaskDependencies(caskConfig)
      } else emptyList()
      caskDAO.deleteCask(caskConfig, force, dependencies)
   }

   /**
    * Clears the contents of given cask
    * For view based cask, this operation does nothing.
    */
   fun clearCask(caskConfig: CaskConfig) {
      if (!caskConfig.exposesType) {
         log().warn("Clearing the contents of table ${caskConfig.tableName} for type ${caskConfig.qualifiedTypeName}")
         caskDAO.emptyCask(caskConfig.tableName)
      } else {
         log().warn("can not clear the content of a view based cask for ${caskConfig.qualifiedTypeName}")
      }
   }

   fun deleteCask(tableName: String, force: Boolean) {
      caskConfigRepository.findByTableName(tableName)?.let {caskConfig ->
         deleteCask(caskConfig, force)
      }
   }

   fun caskIngestionErrorsFor(tableName: String, page: Int, pageSize: Int, start: Instant, end: Instant): CaskIngestionErrorDtoPage {
      // Please note that our query is based on type name not based on table name.
      return caskConfigRepository.findByTableName(tableName)?.let { caskConfig ->
         val result = ingestionErrorRepository
            .findByFullyQualifiedNameAndInsertedAtBetweenOrderByInsertedAtDesc(caskConfig.qualifiedTypeName, start, end, PageRequest.of(page, pageSize))

         val items = result.content.map { ingestionError ->
            CaskIngestionErrorDto(
               caskMessageId = ingestionError.caskMessageId,
               createdAt = ingestionError.insertedAt,
               fqn = ingestionError.fullyQualifiedName,
               error = ingestionError.error)
         }
         CaskIngestionErrorDtoPage(items = items, currentPage = result.number.toLong(), totalItem = result.totalElements, totalPages = result.totalPages.toLong())
      } ?: CaskIngestionErrorDtoPage(listOf(), 0L, 0L, 0L)
   }

   fun caskIngestionMessage(caskMessageId: String): Pair<Resource, ContentType?> {
      return caskDAO.fetchRawCaskMessage(caskMessageId)?.let { (stream, contentType) ->
         ByteArrayResource(stream) to contentType
      } ?:  InputStreamResource(ByteArrayInputStream(ByteArray(0))) to null
   }

   fun deleteCaskByTypeName(typeName: String, force: Boolean) {
      log().info("Deleting cask for type => $typeName")
      caskConfigRepository
         .findAllByQualifiedTypeName(typeName)
         .forEach { caskConfig -> this.deleteCask(caskConfig, force) }
   }

   fun clearCaskByTypeName(typeName: String) {
      log().info("Clearing cask for type => $typeName")
      caskConfigRepository
         .findAllByQualifiedTypeName(typeName)
         .forEach { caskConfig -> this.clearCask(caskConfig) }
   }
}

interface CaskIngestionRequest {
   fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, messageId:String): StreamSource
   val versionedType: VersionedType
   val contentType: ContentType

   // Emits the parameters that this ingestion request has used to be configured.
   // Will be persisted along with the message.
   val parameters:Any

   val debug: Boolean
   val nullValues: Set<String>

   companion object {
      fun fromContentTypeAndHeaders(contentType: ContentType, versionedType: VersionedType, mapper: ObjectMapper, queryParams: MultiValueMap<String, String?>, caskIngestionErrorProcessor: CaskIngestionErrorProcessor): CaskIngestionRequest {
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

private fun  MultiValueMap<String, String?>.toMapOfListWhereMultiple():Map<String,Any?> {
   return this.map { (key,value) ->
      key to when (value.size) {
         0 -> null
         1 -> value.first()
         else -> value
      }
   }.toMap()
}
