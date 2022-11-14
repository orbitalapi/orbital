package io.vyne.schemaServer.core.git

import io.vyne.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import io.vyne.schemaServer.core.file.FileChangeDetectionMethod
import io.vyne.schemaServer.core.file.packages.ReactivePollingFileSystemMonitor
import io.vyne.schemaServer.core.file.packages.ReactiveWatchingFileSystemMonitor
import mu.KotlinLogging
import java.nio.file.Paths
import java.time.Duration

/**
 * Reponsible for creating a GitSchemaPackageLoader from a
 * GitRepositoryConfig
 */
class GitSchemaPackageLoaderFactory(
   private val adaptorFactory: SchemaSourcesAdaptorFactory = SchemaSourcesAdaptorFactory(),

   private val changeDetectionMethod: FileChangeDetectionMethod = FileChangeDetectionMethod.WATCH,
   private val pollFrequency: Duration = Duration.ofSeconds(5L),

   ) {
   private val logger = KotlinLogging.logger {}

   fun build(config: GitSchemaRepositoryConfig, spec: GitRepositoryConfig): GitSchemaPackageLoader {
      val rootPath = config.checkoutRoot ?: Paths.get("./gitWorkingDir")
      val workingDir = rootPath.resolve(spec.name + "/")
      logger.info { "Building a git package loader for git repo at  ${spec.uri} checking out to $workingDir, polling ${config.pollFrequency}" }

      val fileMonitor = when (changeDetectionMethod) {
         FileChangeDetectionMethod.WATCH -> ReactiveWatchingFileSystemMonitor(workingDir, listOf(".git"))
         FileChangeDetectionMethod.POLL -> ReactivePollingFileSystemMonitor(workingDir, pollFrequency)
      }
      return GitSchemaPackageLoader(
         workingDir,
         spec,
         adaptorFactory.getAdaptor(spec.loader),
         fileMonitor,
         config.pollFrequency
      )
   }
}
