package io.vyne.schema.publisher

import arrow.core.Either
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

/**
 * Schema publisher is responsible for taking a provided
 * schema source, and publishing it to the Vyne ecosystem
 */
interface SchemaPublisherTransport {
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

interface AsyncSchemaPublisherTransport : SchemaPublisherTransport {

   /**
    * The flux that emits all responses.
    *
    * Note that because of reconnect semantics, a submission response
    * can come multiple times for a single submission
    *
    */
   val sourceSubmissionResponses: Flux<SourceSubmissionResponse>

   /**
    * Submits a schema to the schema server whenever
    * a connection is established.
    *
    * If the connection is dropped, and re-established, then the schemas
    * are resubmitted upon the new connection being established.
    *
    * The returned Flux is the same as sourceSubmissionResponses, returned
    * here for convenience only.
    */
   fun submitSchemaOnConnection(
      publisherId: String,
      versionedSources: List<VersionedSource>
   ): Flux<SourceSubmissionResponse>


   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      return submitSchemaOnConnection("fixme", versionedSources)
         .blockFirst()!!
         .asEither()
   }


}
