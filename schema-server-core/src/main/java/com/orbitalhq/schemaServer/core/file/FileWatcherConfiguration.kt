package com.orbitalhq.schemaServer.core.file

import com.fasterxml.jackson.annotation.JsonProperty
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.packages.PackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
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
   // Answer: It's nullable when getting a git repo for the first time.
   // In order to load the git repo, we need to create a FileSystemPackageSpec,
   // but we can't know the package identifier until we've loaded the spec.
   // That could do with some work I guess..
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
