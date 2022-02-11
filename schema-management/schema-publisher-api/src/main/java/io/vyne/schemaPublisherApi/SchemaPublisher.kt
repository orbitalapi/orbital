package io.vyne.schemaPublisherApi

import arrow.core.Either
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

/**
 * Schema publisher is responsible for taking a provided
 * schema source, and publishing it to the vyne ecosystem
 */
interface SchemaPublisher {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String) = submitSchema(VersionedSource(schemaName, schemaVersion, schema))
   fun submitSchema(versionedSource: VersionedSource) = submitSchemas(listOf(versionedSource), emptyList())
   fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> = submitSchemas(versionedSources, emptyList())
   fun submitSchemas(versionedSources: List<VersionedSource>, removedSources: List<SchemaId> = emptyList()): Either<CompilationException, Schema>
   val schemaServerConnectionLost: Publisher<Unit>
      get() = Flux.empty()
}
