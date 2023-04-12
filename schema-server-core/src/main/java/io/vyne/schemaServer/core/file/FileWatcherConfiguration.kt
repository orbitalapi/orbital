package io.vyne.schemaServer.core.file

import com.fasterxml.jackson.annotation.JsonProperty
import io.vyne.PackageIdentifier
import io.vyne.schemaServer.packages.PackageLoaderSpec
import io.vyne.schemaServer.packages.TaxiPackageLoaderSpec
import java.nio.file.Path
import java.time.Duration

/**
 * Represents a pointer to a file-based project spec.
 * At it's most simple, is just a path.
 * However, can be configured to allow richer loading (such as by
 * providing an adaptor that converts OpenAPI to Taxi, etc)
 *
 */
data class FileSystemPackageSpec(
   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   val path: Path,
   val loader: PackageLoaderSpec = TaxiPackageLoaderSpec,
   val isEditable: Boolean = false,
   // TODO : Why is this nullable? When should we provide vs omit it?
   val packageIdentifier: PackageIdentifier? = null
) {

   @JsonProperty(value = "path", access = JsonProperty.Access.READ_ONLY)
   val pathString: String = path.toString()

}

data class FileSystemSchemaRepositoryConfig(
   val changeDetectionMethod: FileChangeDetectionMethod = FileChangeDetectionMethod.WATCH,
   val pollFrequency: Duration = Duration.ofSeconds(5L),
   val recompilationFrequencyMillis: Duration = Duration.ofMillis(3000L),
   val incrementVersionOnChange: Boolean = false,
   val projects: List<FileSystemPackageSpec> = emptyList()
)

enum class FileChangeDetectionMethod {
   /**
    * Registers a FileSystem change watcher to be notified of
    * changes.
    *
    * The preferred approach, but does have some issues on Windows
    * systems, especially when running from a docker host (ie.,
    * a docker container running linux, watching a file system
    * that is mounted externally from a windows host).
    */
   WATCH,

   /**
    * Polls the file system periodically for changes
    */
   POLL
}
