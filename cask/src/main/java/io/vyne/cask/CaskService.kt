package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.websocket.CaskWebsocketRequest
import io.vyne.cask.websocket.CsvWebsocketRequest
import io.vyne.cask.websocket.JsonWebsocketRequest
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Files

@Component
class CaskService(val schemaProvider: SchemaProvider,
                  val ingesterFactory: IngesterFactory,
                  val objectMapper: ObjectMapper = jacksonObjectMapper()) {

   data class TypeError(val message: String)
   data class ContentTypeError(val message: String)
   enum class ContentType { json, csv}
   val supportedContentTypes: List<ContentType> = listOf(ContentType.json, ContentType.csv)

   fun resolveContentType(contentTypeName: String): Either<ContentTypeError, ContentType> {
      return try {
         val contentType = ContentType.valueOf(contentTypeName)
         return if (supportedContentTypes.contains(contentType)) {
            Either.right(contentType)
         } else {
            Either.left(ContentTypeError("Unsupported contentType=${contentTypeName}"))
         }
      } catch (e: java.lang.Exception) {
         Either.left(ContentTypeError("Unknown contentType=${contentTypeName}"))
      }
   }

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

   fun ingestRequest(request: CaskWebsocketRequest,
                     inputStream: Flux<InputStream>): Flux<InstanceAttributeSet> {
      val schema = schemaProvider.schema()
      val versionedType = request.versionedType
      val cacheDirectory = Files.createTempDirectory(versionedType.versionedName)

      val streamSource = when (request) {
         is JsonWebsocketRequest -> {
            JsonStreamSource(
               inputStream,
               versionedType,
               schema,
               cacheDirectory,
               objectMapper)
         }
         is CsvWebsocketRequest -> {
            CsvStreamSource(
               input = inputStream,
               type = versionedType,
               schema = schema,
               readCacheDirectory = cacheDirectory,
               csvFormat = request.csvFormat(),
               nullValues = request.nullValues()
            )
         } else -> {
            return Flux.error(NotImplementedError("Ingestion $request not supported!"))
         }
      }

      val ingestionStream = IngestionStream(
         versionedType,
         TypeDbWrapper(versionedType, schema, cacheDirectory, null),
         streamSource)

      val ingester = ingesterFactory.create(ingestionStream)
      ingester.initialize()
      // This code executes every time a new message is pushed to cask
      // So every request we inject cask service schema as a namespace vyne.casks
      // It also crashes vyne
      // TODO find the best place for this logic
      //applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, versionedType))

      return ingester
         .ingest()
   }
}
