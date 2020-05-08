package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.io.File
import java.io.InputStream

@Component
class CaskService(val schemaProvider: SchemaProvider,
                  val ingesterFactory: IngesterFactory,
                  val objectMapper: ObjectMapper = ObjectMapper()) {

    data class TypeError(val message: String)

    fun resolveType(typeReference: String): Either<VersionedType, TypeError> {
        val schema = schemaProvider.schema()
        if (schema.types.isEmpty()) {
            log().warn("Empty schema, no types defined? Check the configuration please!")
            return Either.right(TypeError("Empty schema, no types defined."))
        }

        try {
            // Type[], Type of lang.taxi.Array<OrderSummary>
            // schema.versionedType(lang.taxi.Array) throws error, investigate why
            val versionedTypeReference = VersionedTypeReference.parse(typeReference)
            return Either.left(schema.versionedType(versionedTypeReference))
        } catch (e: Exception) {
            log().error("Type not found typeReference=${typeReference} errorMessage=${e.message}")
            return Either.right(TypeError("Type reference '${typeReference}' not found."))
        }
    }

    fun ingestRequest(versionedType: VersionedType, input: Flux<InputStream>): Flux<InstanceAttributeSet> {
        val schema = schemaProvider.schema()
        val cacheDirectory = File.createTempFile(versionedType.versionedName, "").toPath()
        val streamSource = JsonStreamSource(
                input,
                versionedType,
                schema,
                cacheDirectory,
                objectMapper)

        val ingestionStream = IngestionStream(
                versionedType,
                TypeDbWrapper(versionedType, schema, cacheDirectory, null),
                streamSource)

        val ingester = ingesterFactory.create(ingestionStream)
        ingester.initialize()

        return ingester
                .ingest()
                .doOnError { log().error("Error ", it) }
    }
}
