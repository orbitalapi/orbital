package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.vyne.VersionedTypeReference
import io.vyne.cask.api.*
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.websocket.CsvWebsocketRequest
import io.vyne.cask.websocket.JsonWebsocketRequest
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Component
class CaskService(private val schemaProvider: SchemaProvider,
                  private val ingesterFactory: IngesterFactory,
                  private val caskDAO: CaskDAO) {

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

   fun ingestRequest(request: CaskIngestionRequest, input: Flux<InputStream>): Flux<InstanceAttributeSet> {
      val schema = schemaProvider.schema()
      val versionedType = request.versionedType
      val messageId = UUID.randomUUID().toString()
      val cacheDirectory = createCacheDirectory(versionedType, request, messageId)

      // capturing path to the message
      caskDAO.createCaskMessage(versionedType, messageId, input)

      val streamSource: StreamSource = request.buildStreamSource(
         input = input,
         type = versionedType,
         schema = schema,
         readCacheDirectory = cacheDirectory
      )
      val ingestionStream = IngestionStream(
         versionedType,
         TypeDbWrapper(versionedType, schema, cacheDirectory, null),
         streamSource)

      return ingesterFactory
         .create(ingestionStream)
         .ingest()
   }

   private fun createCacheDirectory(versionedType: VersionedType, request: CaskIngestionRequest, messageId: String): Path {
      // TODO setup folder via config once we start making a use of it
      val caskMessageCache = System.getProperty("java.io.tmpdir")
      val cachePath = Paths.get(
         caskMessageCache,
         versionedType.versionedName,
         request.contentType.name,
         messageId)
      log().info("CaskMessage cachePath=$cachePath")
      Files.createDirectories(cachePath)
      return cachePath
   }

   fun getCasks(): List<CaskConfig> {
      return caskDAO.findAllCaskConfigs()
   }

   fun getCaskDetails(tableName: String): CaskDetails {
      val count = caskDAO.countCaskRecords(tableName)
      return CaskDetails(count)
   }

   fun deleteCask(tableName: String) {
      if (caskDAO.exists(tableName)) {
         caskDAO.deleteCask(tableName)
      }
   }

   fun emptyCask(tableName: String) {
      if (caskDAO.exists(tableName)) {
         caskDAO.emptyCask(tableName)
      }
   }
}

interface CaskIngestionRequest {
   fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, readCacheDirectory: Path): StreamSource
   val versionedType: VersionedType
   val contentType: ContentType

   val debug: Boolean
   val nullValues: Set<String>

   companion object {
      fun fromContentTypeAndHeaders(contentType: ContentType, versionedType: VersionedType, mapper: ObjectMapper, queryParams: MultiValueMap<String, String?>): CaskIngestionRequest {
         return when (contentType) {
             ContentType.csv -> {
                val params = mapper.convertValue<CsvIngestionParameters>(queryParams.toMapOfListWhereMultiple())
                CsvWebsocketRequest(params, versionedType)
             }
             ContentType.json -> {
                val params = mapper.convertValue<JsonIngestionParameters>(queryParams.toMapOfListWhereMultiple())
                JsonWebsocketRequest(params, versionedType, mapper)
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
