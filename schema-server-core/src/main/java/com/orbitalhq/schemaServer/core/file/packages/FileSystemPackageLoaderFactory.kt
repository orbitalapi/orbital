package com.orbitalhq.schemaServer.core.file.packages

import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.file.FileChangeDetectionMethod
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.utils.files.ReactiveFileSystemMonitor
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
import com.orbitalhq.utils.files.ReactiveWatchingFileSystemMonitor
import mu.KotlinLogging
import java.nio.file.Files

/**
 * Responsible for creating a FileSystemPackageLoader
 * from a FileSystemPackageSpec
 */
class FileSystemPackageLoaderFactory(
   private val adaptorFactory: SchemaSourcesAdaptorFactory = SchemaSourcesAdaptorFactory()
) {
   private val logger = KotlinLogging.logger {}

   // design choice:
   // pass the config in the build method, rather than the constructor,
   // as it allows for the config to change
   // (eg., be modified on disk), without having to destroy this class
   fun build(config: FileSystemSchemaRepositoryConfig, spec: FileSystemPackageSpec): FileSystemPackageLoader {
      // Sometimes we're passed a file (ie., taxi.conf), and sometimes it's the directory.
      // In all cases, we want the directory
      val pathToWatch = if (Files.isDirectory(spec.path)) {
         spec.path
      } else {
         spec.path.parent
      }
      logger.info { "Configuring a PackageLoader using a watch method of ${config.changeDetectionMethod} at $pathToWatch" }
      val monitor: ReactiveFileSystemMonitor = when (config.changeDetectionMethod) {
         FileChangeDetectionMethod.POLL -> ReactivePollingFileSystemMonitor(pathToWatch, config.pollFrequency)
         FileChangeDetectionMethod.WATCH -> ReactiveWatchingFileSystemMonitor(pathToWatch)
      }

      val adaptor = adaptorFactory.getAdaptor(spec.loader)
      return FileSystemPackageLoader(
         spec,
         adaptor,
         monitor,
      )
   }
}
