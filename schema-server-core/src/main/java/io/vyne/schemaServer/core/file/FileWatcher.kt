package io.vyne.schemaServer.core.file

import io.vyne.schemaServer.core.file.packages.ReactiveWatchingFileSystemMonitor
import mu.KotlinLogging
import reactor.core.publisher.Sinks
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference


class FileWatcher(
   override val repository: FileSystemSchemaRepository,
   private val schemaCompilationInterval: Duration,
   excludedDirectoryNames: List<String> = Companion.excludedDirectoryNames
) : FileSystemMonitor {

   companion object {
      private val excludedDirectoryNames = listOf(
         ".git",
         "node_modules",
         ".vscode"
      )
   }

   private val logger = KotlinLogging.logger { }
   private val watcher = ReactiveWatchingFileSystemMonitor(repository.projectPath, excludedDirectoryNames)

   override fun start() {
      watcher.startWatching()
         .bufferTimeout(10, schemaCompilationInterval)
         .subscribe { signals ->
            val changedPaths = signals.flatten().distinct()
               .joinToString { it.path.toFile().canonicalPath }
            try {
               logger.info { "Changes detected: $changedPaths - refreshing sources" }
               repository.refreshSources()
            } catch (exception: Exception) {
               logger.error("Exception in compiler service:", exception)
            }
         }
   }

   override fun stop() {
      watcher.stop()
   }


}
