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


data class VersionedSource(
   val name: String,
   val version: String,
   val content: String,
   val packageIdentifier: PackageIdentifier?
) : Serializable {
   constructor(
      name: String, version: String, content: String
   ) : this(splitPackageIdentifier(name).second, version, content, splitPackageIdentifier(name).first)

   val packageQualifiedName = prependPackageIdentifier(packageIdentifier, name)


   companion object {
      private val hashCharset = java.nio.charset.Charset.defaultCharset()
      const val UNNAMED = "<unknown>"
      val DEFAULT_VERSION: Version = Version.valueOf("0.0.0")

      fun prependPackageIdentifier(packageIdentifier: PackageIdentifier?, sourceName: String): String {
         if (packageIdentifier == null) {
            return sourceName
         }
         val packageIdentifierPrefix = "[${packageIdentifier.id}]/"
         return if (sourceName.startsWith(packageIdentifierPrefix)) {
            sourceName
         } else {
            "$packageIdentifierPrefix$sourceName"
         }
      }

      fun splitPackageIdentifier(name: String): Pair<PackageIdentifier?, String> {
         return if (name.contains("]/")) {
            val (packageIdentifier, trimmedName) = name.split("]/")
            return PackageIdentifier.fromId(packageIdentifier.removePrefix("[")) to trimmedName
         } else {
            null to name
         }
      }

      @VisibleForTesting
      fun sourceOnly(content: String) = VersionedSource(UNNAMED, DEFAULT_VERSION.toString(), content)

      fun forIdAndContent(id: SchemaId, content: String): VersionedSource {
         val (name, version) = id.split(":")
         return VersionedSource(name, version, content)
      }

      fun unversioned(name: String, content: String) = VersionedSource(name, DEFAULT_VERSION.toString(), content)


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
      .putString(content, hashCharset)
      .hash()
      .toString()
      .substring(0, 6)

   val fullHash = Hashing.sha256().newHasher()
      .putString(packageQualifiedName, hashCharset)
      .putString(version, hashCharset)
      .putString(content, hashCharset)
      .hash()
      .toString()

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
