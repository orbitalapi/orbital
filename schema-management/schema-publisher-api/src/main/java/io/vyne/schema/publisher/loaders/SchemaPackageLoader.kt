package io.vyne.schema.publisher.loaders

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.publisher.PublisherType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*


data class CreateChangesetResponse(
   val changeset: Changeset
)

data class AddChangesToChangesetResponse(
   val branchOverview: BranchOverview?
)

data class FinalizeChangesetResponse(
   val branchOverview: BranchOverview,
   val changeset: Changeset,
   val link: String? = null,
)

data class UpdateChangesetResponse(
   val changeset: Changeset,
)

data class AvailableChangesetsResponse(
   val changesets: List<Changeset>
)

data class BranchOverview(
   val additions: Int,
   val changedFiles: Int,
   val deletions: Int,
   val author: String,
   val description: String,
   val lastUpdated: Date
)


data class Changeset(
   val name: String,
   val isActive: Boolean,
   val packageIdentifier: PackageIdentifier
)

data class SetActiveChangesetResponse(
   val changeset: Changeset,
   val branchOverview: BranchOverview?
)

/**
 * Loads schema metadata (and often the schema itself) from
 * some location.
 *
 * Responsible for emitting a subtype of PackageMetadata, which should
 * include source files for a downstream SchemaSourcesAdaptor to convert into a SchemaPackage.
 *
 * eg:  Transport that encapsulates loading schemas of some form from a git repository.
 */
interface SchemaPackageTransport {
   /**
    * If called multiple times, the same Flux<> should be returned
    */
   fun start(): Flux<SourcePackage>

   val description: String

   fun listUris(): Flux<URI>
   fun readUri(uri: URI): Mono<ByteArray>

   fun isEditable(): Boolean
   fun createChangeset(name: String): Mono<CreateChangesetResponse>

   // TODO Move these to the proper place (current module is called "publisher")
   fun addChangesToChangeset(name: String, edits: List<VersionedSource>): Mono<AddChangesToChangesetResponse>

   fun finalizeChangeset(name: String): Mono<FinalizeChangesetResponse>

   fun updateChangeset(name: String, newName: String): Mono<UpdateChangesetResponse>

   fun getAvailableChangesets(): Mono<AvailableChangesetsResponse>
   fun setActiveChangeset(branchName: String): Mono<SetActiveChangesetResponse>

   val packageIdentifier: PackageIdentifier

   val publisherType: PublisherType
}

interface SchemaSourcesAdaptor {
   fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata>
   fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage>
}

