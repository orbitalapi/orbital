package io.vyne.schema.publisher

import arrow.core.Either
import arrow.core.right
import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Ids
import lang.taxi.CompilationException
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux


/**
 * Schema publisher is responsible for taking a provided
 * schema source, and publishing it to the Vyne ecosystem
 */
interface SchemaPublisherTransport {
//   fun submitSchema(
//      schemaName: String,
//      schemaVersion: String,
//      schema: String
//   ) = submitSchema(VersionedSource(schemaName, schemaVersion, schema))

   fun submitSchema(packageMetadata: PackageMetadata, versionedSource: VersionedSource) =
      submitSchemas(packageMetadata, listOf(versionedSource))

   /**
    * Allows the transport to enrich a schema submission with KeepAlive information.
    * i.e., Allows an RSocket transport to provide RSocket keep-alive details.
    */
   fun buildKeepAlivePackage(
      submission: SourcePackage,
      publisherId: PublisherId = Ids.id("publisher-")
   ): KeepAlivePackageSubmission {
      return KeepAlivePackageSubmission(
         submission,
         ManualRemoval,
         publisherId
      )
   }

   fun submitSchemas(
      packageMetadata: PackageMetadata,
      versionedSources: List<VersionedSource>
   ): Either<CompilationException, Schema> = submitPackage(SourcePackage(packageMetadata, versionedSources))

   fun submitMonitoredPackage(submission: KeepAlivePackageSubmission): Either<CompilationException, Schema>
   fun submitPackage(submission: SourcePackage): Either<CompilationException, Schema>

   fun submitPackages(packages: List<SourcePackage>): Either<CompilationException, Schema> {
      val results = packages.map { submitPackage(it) }
      return if (results.isEmpty()) {
         TaxiSchema.empty().right()
      } else {
         results.last()
      }
   }

   fun removeSchemas(identifiers: List<PackageIdentifier>): Either<CompilationException, Schema>

//   fun submitSchemas(
//      versionedSources: List<VersionedSource>,
//      removedSources: List<SchemaId> = emptyList()
//   ): Either<CompilationException, Schema>

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
      sourcePackage: SourcePackage,
      publisherId: PublisherId = Ids.id("publisher-")
   ): Flux<SourceSubmissionResponse> =
      submitSchemaOnConnection(
         buildKeepAlivePackage(sourcePackage, publisherId)
      )


   fun submitSchemaOnConnection(submission: KeepAlivePackageSubmission): Flux<SourceSubmissionResponse>

   override fun submitSchemas(
      packageMetadata: PackageMetadata,
      versionedSources: List<VersionedSource>,
   ): Either<CompilationException, Schema> {
      return submitSchemaOnConnection(SourcePackage(packageMetadata, versionedSources))
         .blockFirst()!!
         .asEither()
   }

//   override fun submitSchemaPackage(submission: KeepAlivePackageSubmission): Either<CompilationException, Schema> {
//      return submitSchemaOnConnection(submission)
//         .blockFirst()!!
//         .asEither()
//   }
//
//   override fun submitSchemas(submission: SourcePackage): Either<CompilationException, Schema> {
//      return submitSchemaOnConnection(buildKeepAlivePackage(submission))
//         .blockFirst()!!.asEither()
//   }


//   override fun submitSchemas(
//      versionedSources: List<VersionedSource>,
//      removedSources: List<SchemaId>
//   ): Either<CompilationException, Schema> {
//      return submitSchemaOnConnection("fixme", versionedSources)
//         .blockFirst()!!
//         .asEither()
//   }


}
