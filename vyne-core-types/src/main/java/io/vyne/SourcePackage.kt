package io.vyne

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiPackageSources
import java.io.Serializable
import java.time.Instant


data class SourcePackage(
   val packageMetadata: PackageMetadata,
   val sources: List<VersionedSource>
):Serializable {
   val identifier = packageMetadata.identifier
}


typealias UnversionedPackageIdentifier = String

data class PackageIdentifier(
   val organisation: String,
   val name: String,

   /**
    * Will be parsed to Either a Semver or a git-like SHA
    */
   val version: String,
): Serializable {
   val unversionedId: UnversionedPackageIdentifier = "$organisation/$name"
   val id = "$unversionedId/$version"

   companion object {
      fun fromId(id: String): PackageIdentifier {
         val parts = id.split("/")
         require(parts.size == 3) { "Invalid id provided ($id) - expected three parts with / delimiters" }
         val (organisation, name, version) = parts
         return PackageIdentifier(organisation, name, version)
      }

      fun toUriSafeId(identifier: PackageIdentifier):String {
         return identifier.toString().replace("/", ":")
      }
      fun fromUriSafeId(uriSafeId:String):PackageIdentifier {
         return fromId(uriSafeId.replace(":","/"))
      }
   }


   override fun toString(): String = id
}


data class ParsedPackage(
   val metadata: PackageMetadata,
   val sources: List<ParsedSource>
):Serializable {
   val isValid = sources.all { it.isValid }
   val identifier = metadata.identifier
   val sourcesWithErrors = sources.filter { !it.isValid }

   fun toSourcePackage(): SourcePackage {
      return SourcePackage(metadata,
         sources.map { it.source })
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

fun TaxiPackageSources.asSourcePackage():SourcePackage {
   return SourcePackage(
      this.project.toPackageMetadata(),
      this.versionedSources()
   )
}
