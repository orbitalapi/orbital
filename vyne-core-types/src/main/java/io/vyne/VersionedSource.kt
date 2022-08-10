package io.vyne

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.zafarkhaja.semver.Version
import com.google.common.annotations.VisibleForTesting
import com.google.common.hash.Hashing
import io.vyne.utils.log
import lang.taxi.CompilationError
import lang.taxi.errors
import lang.taxi.packages.TaxiPackageSources
import lang.taxi.sources.SourceCode
import java.io.Serializable
import java.time.Instant


data class VersionedSource(val name: String, val version: String, val content: String) : Serializable {
   fun prependPackageIdentifier(packageIdentifier: PackageIdentifier): VersionedSource {
      val packageIdentifierPrefix = "[${packageIdentifier.id}]/"
      return if (this.name.startsWith(packageIdentifierPrefix)) {
         this
      } else {
         this.copy(name = "$packageIdentifierPrefix$name")
      }
   }

   fun removePackageIdentifier(): Pair<PackageIdentifier?, VersionedSource> {
      return if (this.name.contains("]/")) {
         val (packageIdentifier, trimmedName) = this.name.split("]/")
         return PackageIdentifier.fromId(packageIdentifier.removePrefix("[")) to this.copy(name = trimmedName)
      } else {
         null to this
      }
   }

   companion object {
      const val UNNAMED = "<unknown>"
      val DEFAULT_VERSION: Version = Version.valueOf("0.0.0")

      @VisibleForTesting
      fun sourceOnly(content: String) = VersionedSource(UNNAMED, DEFAULT_VERSION.toString(), content)

      fun forIdAndContent(id: SchemaId, content: String): VersionedSource {
         val (name, version) = id.split(":")
         return VersionedSource(name, version, content)
      }

      fun unversioned(name: String, content: String) = VersionedSource(name, DEFAULT_VERSION.toString(), content)

      fun nameAndVersionFromId(id: SchemaId): Pair<String, String> {
         val parts = id.split(":")
         require(parts.size == 2)
         return parts[0] to parts[1]
      }

      fun fromTaxiSourceCode(sourceCode: SourceCode, version: String = DEFAULT_VERSION.toString()): VersionedSource {
         return VersionedSource(sourceCode.normalizedSourceName, version, sourceCode.content)
      }
   }

   val id: SchemaId = "$name:$version"

   @Transient
   private var _semver: Version? = null

   @get:JsonIgnore
   val semver: Version
      get() {
         if (_semver == null) {
            _semver = try {
               Version.valueOf(version)
            } catch (exception: Exception) {
               log().warn("Schema $name has an invalid version of $version.  Will use default Semver, with current time.  Newest wins.");
               DEFAULT_VERSION.setBuildMetadata(Instant.now().epochSecond.toString());
            }
         }
         return _semver ?: error("Semver failed to initialize")
      }

   val contentHash: String = Hashing.sha256().newHasher()
      .putString(content, java.nio.charset.Charset.defaultCharset())
      .hash()
      .toString()
      .substring(0, 6)

}

@Deprecated("Use PackageIdentifier")
typealias SchemaId = String

data class ParsedSource(val source: VersionedSource, val errors: List<CompilationError> = emptyList()) : Serializable {
   val isValid = errors.errors().isEmpty()

   val name = source.name
}

fun TaxiPackageSources.versionedSources(): List<VersionedSource> {
   return this.sources.map { source -> source.asVersionedSource(this.project.version) }
}

fun SourceCode.asVersionedSource(version: String = VersionedSource.DEFAULT_VERSION.toString()): VersionedSource {
   return VersionedSource(this.sourceName, version, this.content)
}
