package io.vyne.schemaServer.core.file

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
   private val excludedDirectoryNames: List<String> = Companion.excludedDirectoryNames
) : FileSystemMonitor {

   private val logger = KotlinLogging.logger { }
   private var watcherThread: Thread? = null

   data class RecompileRequestedSignal(val path: Path)

   private val sink = Sinks.many().unicast().onBackpressureBuffer<RecompileRequestedSignal>()

   init {
      sink.asFlux()
         .bufferTimeout(10, schemaCompilationInterval)
         .subscribe { signals ->
            val changedPaths = signals.distinct()
               .joinToString { it.path.toFile().canonicalPath }
            try {
               logger.info { "Changes detected: $changedPaths - refreshing sources" }
               repository.refreshSources()
            } catch (exception: Exception) {
               logger.error("Exception in compiler service:", exception)
            }
         }

   }

   override fun start() {
      watch()
   }

   override fun stop() {
      unregisterKeys()
      cancelWatch()
      watcherThread?.interrupt()
   }


   companion object {
      private val excludedDirectoryNames = listOf(
         ".git",
         "node_modules",
         ".vscode"
      )
      private val registeredKeys = ArrayList<WatchKey>()
      private val watchServiceRef = AtomicReference<WatchService>()
   }

   fun cancelWatch() {
      watchServiceRef.get()?.close()
   }

   private fun registerKeys(watchService: WatchService) {
      val path: Path = repository.projectPath

      path.toFile().walkTopDown()
         .onEnter {
            val excluded = (it.isDirectory && excludedDirectoryNames.contains(it.name))
            !excluded
         }
         .filter { it.isDirectory }
         .forEach { directory ->
            watchDirectory(directory.toPath(), watchService)
         }
   }

   private fun watchDirectory(path: Path, watchService: WatchService) {
      registeredKeys.add(
         path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.OVERFLOW
         )
      )
   }

   private fun unregisterKeys() {
      registeredKeys.apply {
         forEach {
            it.cancel()
         }
         clear()
      }
   }

   fun watch(): Thread {
      this.watcherThread = Thread(Runnable {
         logger.info("Starting to watch ${repository.identifier}")
         val watchService = FileSystems.getDefault().newWatchService()

         watchServiceRef.set(watchService)
         registerKeys(watchService)

         try {
            while (true) {
               val key = watchService.take()
               key.pollEvents()
                  .mapNotNull { it.context() as? Path }
                  .filter { path ->
                     val resolvedPath = (key.watchable() as Path).resolve(path)
                     if (Files.isDirectory(resolvedPath)) {
                        logger.info { "Directory change at ${resolvedPath}, adding to watchlist" }
                        watchDirectory(resolvedPath, watchService)
                        val hasTaxi = Files.walk(resolvedPath, Int.MAX_VALUE).anyMatch { path ->
                           path.fileName.toString().endsWith(".taxi")
                        }
                        hasTaxi
                     } else {
                        !path.fileName.toString().contains(".git") &&
                           path.fileName.toString().endsWith(".taxi")
                     }
                  }
                  .forEach { path ->
                     val resolvedPath = (key.watchable() as Path).resolve(path)
                     val message = RecompileRequestedSignal(resolvedPath)
                     sink.emitNext(message) { signalType, emitResult ->
                        logger.warn { "A file change was detected at $path, but emitting the change signal failed: $signalType $emitResult" }
                        false
                     }
                  }
               key.reset()
            }
         } catch (e: ClosedWatchServiceException) {
            logger.warn(e) { "Watch service was closed. ${e.message}" }
         } catch (e: Exception) {
            logger.error(e) { "Error in watch service: ${e.message}" }
         }
      })
      watcherThread!!.start()
      return watcherThread!!
   }
}
