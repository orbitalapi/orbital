package io.vyne.schema.publisher

import arrow.core.Either
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Schema publisher is responsible for taking a provided
 * schema source, and publishing it to the vyne ecosystem
 */
interface SchemaPublisher {
   fun submitSchema(
      schemaName: String,
      schemaVersion: String,
      schema: String
   ) = submitSchema(VersionedSource(schemaName, schemaVersion, schema))

   fun submitSchema(versionedSource: VersionedSource) = submitSchemas(listOf(versionedSource), emptyList())
   fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> =
      submitSchemas(versionedSources, emptyList())

   fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId> = emptyList()
   ): Either<CompilationException, Schema>

   val schemaServerConnectionLost: Publisher<Unit>
      get() = Flux.empty()
}

interface AsyncSchemaPublisher : SchemaPublisher {
   fun submitSchemaAsync(
      schemaName: String,
      schemaVersion: String,
      schema: String
   ) = submitSchemaAsync(VersionedSource(schemaName, schemaVersion, schema))

   fun submitSchemaAsync(versionedSource: VersionedSource) = submitSchemasAsync(listOf(versionedSource), emptyList())
   fun submitSchemasAsync(versionedSources: List<VersionedSource>) = submitSchemasAsync(versionedSources, emptyList())
   fun submitSchemasAsync(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId> = emptyList()
   ): Mono<Either<CompilationException, Schema>>


   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      return submitSchemasAsync(versionedSources, removedSources).block(Duration.ofSeconds(10))!!
   }
}
