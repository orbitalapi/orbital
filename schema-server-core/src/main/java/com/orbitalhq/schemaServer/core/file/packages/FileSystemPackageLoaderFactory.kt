package com.orbitalhq.schemaServer.core.file.packages

import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.file.FileChangeDetectionMethod
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.utils.files.ReactiveFileSystemMonitor
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
import com.orbitalhq.utils.files.ReactiveWatchingFileSystemMonitor
import mu.KotlinLogging

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
      logger.info { "Configuring a PackageLoader using a watch method of ${config.changeDetectionMethod} at ${spec.path}" }
      val monitor: ReactiveFileSystemMonitor = when (config.changeDetectionMethod) {
         FileChangeDetectionMethod.POLL -> ReactivePollingFileSystemMonitor(spec.path, config.pollFrequency)
         FileChangeDetectionMethod.WATCH -> ReactiveWatchingFileSystemMonitor(spec.path)
      }

      val adaptor = adaptorFactory.getAdaptor(spec.loader)
      return FileSystemPackageLoader(
         spec,
         adaptor,
         monitor,
      )
   }
}
