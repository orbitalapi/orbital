package io.vyne.schemaServer.core.file.packages

import io.vyne.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import io.vyne.schemaServer.core.file.*
import mu.KotlinLogging

/**
 * Responsible for creating a FileSystemPackageLoader
 * from a FileSystemPackageSpec
 */
class FileSystemPackageLoaderFactory {
   private val logger = KotlinLogging.logger {}

   // design choice:
   // pass the config in the build method, rather than the constructor,
   // as it allows for the config to change
   // (eg., be modified on disk), without having to destroy this class
   fun build(config: FileSystemSchemaRepositoryConfig, spec: FileSystemPackageSpec): FileSystemPackageLoader {
      logger.info { "Configuring a PackageLoader using a watch method of ${config.changeDetectionMethod} at ${spec.path}" }
      val monitor: ReactiveFileSystemMonitor = when (config.changeDetectionMethod) {
         FileChangeDetectionMethod.POLL -> ReactivePollingFileSystemMonitor(spec.path, config.pollFrequency)
         FileChangeDetectionMethod.WATCH -> ReactiveWatchingFileSystemMonitor(spec.path)
      }
      return FileSystemPackageLoader(
         spec,
         TaxiSchemaSourcesAdaptor(),
         monitor,
      )
   }
}
