package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.io.File
import java.io.InputStream

@Component
class CaskService(val schemaProvider: SchemaProvider,
                  val ingesterFactory: IngesterFactory,
                  val objectMapper: ObjectMapper = jacksonObjectMapper()) {

   data class TypeError(val message: String)
   data class MediaTypeError(val message: String)

   val supportedContentTypes: List<MediaType> = listOf(MediaType.APPLICATION_JSON)

   fun resolveContentType(contentTypeName: String): Either<MediaTypeError, MediaType> {
      // https://www.iana.org/assignments/media-types/media-types.xhtml
      return try {
         val contentType = MediaType.parseMediaType(contentTypeName)
         return if (supportedContentTypes.contains(contentType)) {
            Either.right(contentType)
         } else {
            Either.left(MediaTypeError("Unsupported contentType=${contentTypeName}"))
         }
      } catch (e: java.lang.Exception) {
         Either.left(MediaTypeError("Unknown contentType=${contentTypeName}"))
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

   fun ingestRequest(versionedType: VersionedType,
                     inputStream: Flux<InputStream>,
                     contentType: MediaType = MediaType.APPLICATION_JSON): Flux<InstanceAttributeSet> {
      val schema = schemaProvider.schema()
      val cacheDirectory = File.createTempFile(versionedType.versionedName, "").toPath()

      val streamSource = when (contentType) {
         MediaType.APPLICATION_JSON -> {
            JsonStreamSource(
               inputStream,
               versionedType,
               schema,
               cacheDirectory,
               objectMapper)
         }
         else -> {
            return Flux.error(NotImplementedError("Ingestion of contentType=${contentType} not supported!"))
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
