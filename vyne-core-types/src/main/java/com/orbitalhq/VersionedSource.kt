package com.orbitalhq

import ch.qos.logback.classic.spi.ThrowableProxy
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.zafarkhaja.semver.Version
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Throwables
import com.google.common.hash.Hashing
import com.orbitalhq.utils.log
import com.orbitalhq.utils.orElse
import lang.taxi.CompilationError
import lang.taxi.errors
import lang.taxi.formatter.TaxiCodeFormatter
import lang.taxi.packages.TaxiPackageSources
import lang.taxi.sources.SourceCode
import lang.taxi.sources.SourceCodeLanguage
import lang.taxi.sources.SourceCodeLanguages
import mu.KotlinLogging
import java.io.Serializable
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import kotlin.io.path.toPath

private val logger = KotlinLogging.logger {}

/**
 * Identifies a VersionedSource file
 * within a package.
 */
data class PackageSourceName(
   val packageIdentifier: PackageIdentifier,
   val sourceName: String
) {
   val packageQualifiedName = VersionedSource.prependPackageIdentifier(packageIdentifier, sourceName)
}


@kotlinx.serialization.Serializable
data class VersionedSource(
   val name: String,
   // TODO : Can we just remove this constructor?
   @Deprecated("use packageIdentifier.source instead")
   val version: String,
   val content: String,
   val packageIdentifier: PackageIdentifier?,
   val language: SourceCodeLanguage = SourceCodeLanguages.TAXI,
   // Don't use java.nio.Path, as it's not serializable
   val path: String? = null
) : Serializable {
   constructor(
      name: String, version: String, content: String, language: SourceCodeLanguage = SourceCodeLanguages.TAXI, path: String? = null
   ) : this(
      splitPackageIdentifier(name).second,
      version,
      content,
      splitPackageIdentifier(name).first,
      language = language,
      path = path
   )

   constructor(
      name: PackageSourceName, content: String, language: SourceCodeLanguage = SourceCodeLanguages.TAXI, path: String? = null
   ) : this(
      name.sourceName,
      name.packageIdentifier.version, content,
      name.packageIdentifier,
      language = language,
      path = path
   )

   val packageQualifiedName = prependPackageIdentifier(packageIdentifier, name)

   fun formattedContent():String {
      return TaxiCodeFormatter.format(content)
   }

   /**
    * Returns a path constructed from the defined path (if present),
    * or the name otherwise.
    */
   @get:JsonIgnore
   val pathOrName:Path
      get() {
         return this.path?.let { Paths.get(it) }.orElse(Paths.get(name))
      }

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
   @kotlinx.serialization.Transient
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

fun TaxiPackageSources.versionedSources(relativeTo: Path?): List<VersionedSource> {

   return this.sources.map { source ->
      source.asVersionedSource(this.project.version, relativeTo)
   }
}

fun SourceCode.asVersionedSource(version: String = VersionedSource.DEFAULT_VERSION.toString(), relativeTo: Path? = null): VersionedSource {
   // Make the name relative to the srcRoot of the project
   val name = if (relativeTo != null) {
      try {
         relativeTo.relativize(URI.create(this.sourceName).toPath()).toString()
      } catch (e:Exception) {
         val rootCause = Throwables.getRootCause(e)
         logger.warn { "Failed to make sourceName ${this.sourceName} relative to path ${relativeTo}, will use original - ${rootCause.message}" }
         this.sourceName
      }

   } else this.sourceName

   return VersionedSource(name, version, this.content, path = this.path?.toString())
}

fun VersionedSource.asTaxiSource(): SourceCode {
   return SourceCode(
      sourceName = this.name,
      content = content
   )
}

fun List<VersionedSource>.asTaxiSource(): List<SourceCode> = this.map { it.asTaxiSource() }
