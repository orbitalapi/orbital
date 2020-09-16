package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.vyne.VersionedTypeReference
import io.vyne.cask.api.*
import io.vyne.cask.config.CaskConfigRepository
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
                  private val caskConfigRepository: CaskConfigRepository,
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
   fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, messageId:String): StreamSource
   val versionedType: VersionedType
   val contentType: ContentType

   // Emits the parameters that this ingestion request has used to be configured.
   // Will be persisted along with the message.
   val parameters:Any

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
