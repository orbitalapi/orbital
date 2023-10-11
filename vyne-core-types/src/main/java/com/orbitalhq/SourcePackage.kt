package com.orbitalhq

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.orbitalhq.models.serde.InstantSerializer
import com.orbitalhq.utils.shaHash
import lang.taxi.packages.SourcesType
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiPackageSources
import mu.KotlinLogging
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

@kotlinx.serialization.Serializable
data class PathGlob(val basePath: Path, val glob: String) {

   fun <T> mapEachDirectoryEntry(action: (Path) -> T): Map<Path, T> {
      val result = mutableMapOf<Path, T>()
      // After messing about, glob:${glob} doesn't work, but glob:**/${glob} does.
      // Suspect this needs more digging later...
      val pathMatcher = this.basePath.fileSystem.getPathMatcher("glob:**/${glob}")
      Files.walk(basePath)
         .filter { path -> pathMatcher.matches(path) }
         .forEach { path ->
            try {
               result[path] = action(path)
            } catch (e: Exception) {
               logger.error(e) { "Failed to process path at $path: ${e.message}" }
            }

         }
      return result
   }
}

@kotlinx.serialization.Serializable
data class SourcePackage(
   val packageMetadata: PackageMetadata,
   /**
    * Contains the sources as they were provided.
    * It's preferrable to read from sourcesWithPackageIdentifier,
    * which contains sources guaranteed to have the package identifier correctly set.
    */
   val sources: List<VersionedSource>,

   /**
    * Additional sources (eg., config, pipelines, extensions, etc).
    */
   val additionalSources: Map<SourcesType, List<VersionedSource>> = emptyMap(),
) : Serializable {
   val identifier = packageMetadata.identifier

   /**
    * The languages present within this source package.
    * Note that at present, it is invalid for a package to expose sources in
    * more than one language, as it makes loading complex, though we can revisit this later if required.
    */
   val languages = sources.map { it.language }.distinct()

   // Note: Originally this was a lazy property.
   // However, it was generating duplicates, (ie., a sources.size == 1, sourceswithPackageIdentitfier.size == 2)
   // and I couldn't work out why.  Can revisit if this needs to become lazy.
   @get:JsonIgnore
   val sourcesWithPackageIdentifier: List<VersionedSource> =
      this.sources.map { it.copy(packageIdentifier = this.packageMetadata.identifier) }

   companion object {
      fun withAdditionalSources(
         packageMetadata: PackageMetadata,
         /**
          * Contains the sources as they were provided.
          * It's preferrable to read from sourcesWithPackageIdentifier,
          * which contains sources guaranteed to have the package identifier correctly set.
          */
         sources: List<VersionedSource>,

         /**
          * Additional sources (eg., config, pipelines, extensions, etc).
          * These aren't actively loaded, and it's left to the appropriate extensions to pull these in
          */
         additionalSourcePaths: List<Pair<SourcesType, PathGlob>> = emptyList()
      ): SourcePackage {
         val additionalSources = additionalSourcePaths.associate { (sourceType, pathGlob) ->
            val sources = pathGlob.mapEachDirectoryEntry { path ->
               VersionedSource(
                  path.toString(),
                  packageMetadata.identifier.version,
                  path.readText(),
                  packageMetadata.identifier
               )
            }.values.toList()
            sourceType to sources
         }
         return SourcePackage(packageMetadata, sources, additionalSources)
      }
   }
}

object SourcePackageHasher {
   fun hash(sourcePackage: SourcePackage): String {
      return sourcePackage.sources.sortedBy { it.name }
         .map { it.fullHash }
         .shaHash()
   }

   fun hash(packages: List<SourcePackage>): String {
      return packages.sortedBy { it.packageMetadata.identifier.id }
         .map { hash(it) }
         .shaHash()
   }
}

typealias UnversionedPackageIdentifier = String
typealias UriSafePackageIdentifier = String

@kotlinx.serialization.Serializable
data class PackageIdentifier(
   val organisation: String,
   val name: String,

   /**
    * Will be parsed to Either a Semver or a git-like SHA
    */
   val version: String,
) : Serializable {
   // Use getters, rather that initializers, as Jackson deser seems to break the initalization
   @JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val unversionedId: UnversionedPackageIdentifier = "$organisation/$name"

   @JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val id: String = "$unversionedId/$version"

   @JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val uriSafeId = toUriSafeId(this)

   companion object {
      /**
       * Generates a package id from a slash delimited id.
       * eg:
       * com.acme/foo/1.0.0
       */
      fun fromId(id: String): PackageIdentifier {
         val parts = id.split("/")
         require(parts.size == 3) { "Invalid id provided ($id) - expected three parts with / delimiters" }
         val (organisation, name, version) = parts
         return PackageIdentifier(organisation, name, version)
      }

      fun toUriSafeId(identifier: PackageIdentifier): UriSafePackageIdentifier {
         return identifier.toString().replace("/", ":")
      }

      /**
       * Generates a package id from a colon delimited id
       * eg:
       * com.acme:foo:1.0.0
       */
      fun fromUriSafeId(uriSafeId: UriSafePackageIdentifier): PackageIdentifier {
         return fromId(uriSafeId.replace(":", "/"))
      }

      fun uriSafeIdToUnversionedIdentifier(uriSafeId: UriSafePackageIdentifier): UnversionedPackageIdentifier {
         val (org, name) = uriSafeId.split(":")
         return "$org/$name"
      }
   }


   override fun toString(): String = id
}


data class ParsedPackage(
   val metadata: PackageMetadata,
   val sources: List<ParsedSource>,
   /**
    * Additional sources (eg., config, pipelines, extensions, etc).
    * These aren't actively loaded, and it's left to the appropriate extensions to pull these in
    */
   val additionalSources: Map<SourcesType, List<VersionedSource>>
) : Serializable {
   val isValid = sources.all { it.isValid }
   val identifier = metadata.identifier
   val sourcesWithErrors = sources.filter { !it.isValid }

   fun toSourcePackage(): SourcePackage {
      return SourcePackage(
         metadata,
         sources.map { it.source },
         this.additionalSources
      )
   }
}


// Have split this from the SchemaPackage so we can allow
// Adaptors that take PackageMetadata, and produce a SchemaPackage.
// Eg: OpenApi PackageMetadata will specify Metadata, but the Adaptor will provide the translation to sources.
@JsonDeserialize(`as` = DefaultPackageMetadata::class)
interface PackageMetadata : Serializable {
   val identifier: PackageIdentifier

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    */
   val submissionDate: Instant
   val dependencies: List<PackageIdentifier>


   companion object {
      fun from(
         identifier: PackageIdentifier,
         submissionDate: Instant = Instant.now(),
         dependencies: List<PackageIdentifier> = emptyList()
      ): PackageMetadata = DefaultPackageMetadata(
         identifier, submissionDate, dependencies
      )

      fun from(
         organisation: String,
         name: String,
         version: String = VersionedSource.DEFAULT_VERSION.toString()
      ) = from(PackageIdentifier(organisation, name, version))
   }

}

// MP:10-APR-23 This class was open, not sure why, made it a data class, as needed an equals and hashcode impl
// for serde (see QueryMessageTest)
@kotlinx.serialization.Serializable
data class DefaultPackageMetadata(
   override val identifier: PackageIdentifier,

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    */
   @kotlinx.serialization.Serializable(with = InstantSerializer::class)
   override val submissionDate: Instant = Instant.now(),
   override val dependencies: List<PackageIdentifier> = emptyList()
) : PackageMetadata

fun lang.taxi.packages.PackageIdentifier.toVynePackageIdentifier(): PackageIdentifier {
   return PackageIdentifier(
      this.name.organisation,
      this.name.name,
      this.version.toString()
   )
}

fun TaxiPackageProject.toPackageMetadata(): PackageMetadata {
   return DefaultPackageMetadata(
      identifier = this.identifier.toVynePackageIdentifier(),
      submissionDate = Instant.now(),
      dependencies = this.dependencyPackages.map { it.toVynePackageIdentifier() }
   )
}

fun TaxiPackageSources.asSourcePackage(): SourcePackage {
   return SourcePackage.withAdditionalSources(
      this.project.toPackageMetadata(),
      this.versionedSources(),
      this.pathGlobs()
   )
}


fun List<SourcePackage>.toSourcesWithPackageIdentifier(): List<VersionedSource> =
   this.flatMap { it.sourcesWithPackageIdentifier }

fun TaxiPackageSources.pathGlobs(): List<Pair<SourcesType, PathGlob>> {
   if (this.project.packageRootPath == null && this.project.additionalSources.isNotEmpty()) {
      error("Additional sources are defined, but no base path has been set")
   }
   return this.project.additionalSources.map { (sourcesType, glob) ->
      sourcesType to PathGlob(this.project.packageRootPath!!, glob)
   }
}
