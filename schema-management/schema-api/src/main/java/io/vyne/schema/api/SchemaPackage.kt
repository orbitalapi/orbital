package io.vyne.schema.api

import io.vyne.VersionedSource
import java.time.Instant

/**
 * New in Schemas3
 *
 * A single package of sources, published by a system.
 * Previously, systems just published a collection of VersionedSources, without
 * any additional metadata attached.
 *
 * We now allow systems to provide versioning information about their sets of schemas,
 * as well as describing dependencies.
 *
 * It is expected that the VersionedSources being published are all taxi files, and
 * that transpilation has happened upstream
 */
data class SchemaPackage(
   val metadata: PackageMetadata,
   // Design choice: There's no real need for these to be VersionedSources anymore, since the
   // Version is duplicated in this SchemaPacakge's PackageIdentifier.
   // However, VersionedSource is so heavily used, that refactoring this doesn't yield any real benefit
   val sources: List<VersionedSource>
) {
   val identifier = metadata.identifier
}

// Have split this from the SchemaPackage so we can allow
// Adaptors that take PackageMetadata, and produce a SchemaPackage.
// Eg: OpenApi PackageMetadata will specify Metadata, but the Adaptor will provide the translation to sources.
interface PackageMetadata {
   val identifier: PackageIdentifier

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    */
   val submissionDate: Instant
   val dependencies: List<PackageIdentifier>
}

open class DefaultPackageMetadata(
   override val identifier: PackageIdentifier,

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    */
   override val submissionDate: Instant = Instant.now(),
   override val dependencies: List<PackageIdentifier> = emptyList()
) : PackageMetadata

data class PackageIdentifier(
   val organisation: String,
   val name: String,

   /**
    * Will be parsed to Either a Semver or a git-like SHA
    */
   val version: String,
)
