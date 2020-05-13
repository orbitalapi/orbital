package io.vyne

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.zafarkhaja.semver.Version
import com.google.common.annotations.VisibleForTesting
import io.vyne.utils.log
import io.vyne.utils.orElse
import lang.taxi.CompilationError
import java.io.Serializable
import java.time.Instant

data class VersionedSource(val name: String, val version: String, val content: String) : Serializable {
   companion object {
      const val UNNAMED = "<unknown>"
      val DEFAULT_VERSION: Version = Version.valueOf("0.0.0")

      @VisibleForTesting
      fun sourceOnly(content: String) = VersionedSource(UNNAMED, DEFAULT_VERSION.toString(), content)

      fun forIdAndContent(id: SchemaId, content: String): VersionedSource {
         val (name, version) = id.split(":")
         return VersionedSource(name, version, content)
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
}

typealias SchemaId = String

data class ParsedSource(val source: VersionedSource, val errors: List<CompilationError> = emptyList()) : Serializable {
   val isValid = errors.isEmpty()

   val name = source.name
}
