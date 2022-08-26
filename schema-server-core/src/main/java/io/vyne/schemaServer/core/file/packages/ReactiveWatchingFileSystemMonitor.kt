package io.vyne.schemaServer.core.file.packages

import io.vyne.schemaServer.core.file.FileWatcher
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.*
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class ReactiveWatchingFileSystemMonitor(
   private val path: Path,
   private val excludedDirectoryNames: List<String> = emptyList()
) : ReactiveFileSystemMonitor {


   private val logger = KotlinLogging.logger { }
   private var watcherThread: Thread? = null
   private val registeredKeys = ArrayList<WatchKey>()
   private val watchServiceRef = AtomicReference<WatchService>()

   private val sink = Sinks.many().replay().latest<List<FileSystemChangeEvent>>()

   override fun startWatching(): Flux<List<FileSystemChangeEvent>> {
      this.watcherThread = watch()
      return sink.asFlux().doOnCancel {
         logger.info { "Cancelling subscription" }
         this.stop()
      }
   }

   fun stop() {
      unregisterKeys()
      cancelWatch()
      sink.tryEmitComplete()
      watcherThread?.interrupt()
   }


   fun cancelWatch() {
      watchServiceRef.get()?.close()
   }

   private fun registerKeys(watchService: WatchService) {
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

   private fun watch(): Thread {
      val thread = Thread {
         logger.info("Starting to watch $path")
         val watchService = FileSystems.getDefault().newWatchService()

         watchServiceRef.set(watchService)
         registerKeys(watchService)

         try {
            while (true) {
               val key = watchService.take()
               val events = key.pollEvents()
                  .mapNotNull { event ->
                     (event.context() as? Path?)?.let { path ->
                        val eventType = when (event.kind()) {
                           StandardWatchEventKinds.ENTRY_CREATE -> FileSystemChangeEvent.FileSystemChangeEventType.FileCreated
                           StandardWatchEventKinds.ENTRY_DELETE -> FileSystemChangeEvent.FileSystemChangeEventType.FileDeleted
                           else -> FileSystemChangeEvent.FileSystemChangeEventType.Unspecified
                        }
                        val resolvedPath = (key.watchable() as Path).resolve(path)
                        FileSystemChangeEvent(resolvedPath, eventType)
                     }
                  };
               sink.emitNext(events) { signalType, emitResult ->
                  logger.warn { "A file change was detected at $path, but emitting the change signal failed: $signalType $emitResult" }
                  false
               }
               key.reset()
            }
         } catch (e: ClosedWatchServiceException) {
            logger.warn(e) { "Watch service was closed. ${e.message}" }
         } catch (e: Exception) {
            logger.error(e) { "Error in watch service: ${e.message}" }
         }
      }
      thread.start()
      return thread
   }

}
