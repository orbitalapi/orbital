package io.vyne.schemaServer

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import reactor.core.publisher.EmitterProcessor
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Component
@ConditionalOnProperty(
   name = ["taxi.change-detection-method"],
   havingValue = "watch",
   matchIfMissing = true
)
class FileWatcherInitializer(val watcher: FileWatcher) {

   @PostConstruct
   fun start() {
      watcher.watch()
   }

}

@Component
@ConditionalOnProperty(
   name = ["taxi.change-detection-method"],
   havingValue = "watch",
   matchIfMissing = true
)
class FileWatcher(
   private val fileSystemVersionedSourceLoader: FileSystemVersionedSourceLoader,
   @Value("\${taxi.schema-recompile-interval-seconds:3}") private val schemaRecompileIntervalSeconds: Long,
   @Value("\${taxi.schema-increment-version-on-recompile:true}") private val incrementVersionOnRecompile: Boolean,
   private val compilerService: CompilerService,
   private val excludedDirectoryNames: List<String> = FileWatcher.excludedDirectoryNames
) {

   private val logger = KotlinLogging.logger { }

   data class RecompileRequestedSignal(val path: Path)

   private val emitter = EmitterProcessor.create<RecompileRequestedSignal>()

   private var active: Boolean = false

   // Can't use active with private setter here, as the @Component
   // annotation makes this class open,  and private setters in open classes is forbidden by the compiler.
   val isActive: Boolean
      get() {
         return active
      }

   init {
      val duration = if (schemaRecompileIntervalSeconds > 0) {
         Duration.ofSeconds(schemaRecompileIntervalSeconds)
      } else {
         // Value must be positive, so use 1 milli
         Duration.ofMillis(1)
      }
      emitter
         .bufferTimeout(10, duration)
         .subscribe { signals ->
            val changedPaths = signals.distinct()
               .joinToString { it.path.toFile().canonicalPath }
            try {
               logger.info { "Changes detected: $changedPaths - recompiling" }
               val sources = fileSystemVersionedSourceLoader.loadVersionedSources(incrementVersionOnRecompile)
               compilerService.recompile(sources)
            } catch (exception: Exception) {
               logger.error("Exception in compiler service:", exception)
            }
         }

   }


   @PreDestroy
   fun destroy() {
      unregisterKeys()
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
      val path: Path = Paths.get(fileSystemVersionedSourceLoader.projectHome)

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

   @Async
   fun watch() {
      if (fileSystemVersionedSourceLoader.projectHome.isEmpty()) {
         logger.warn("schema-local-storage parameter in config file is empty, skipping.")
         return
      }

      logger.info("Starting to watch ${fileSystemVersionedSourceLoader.projectHome}")
      val watchService = FileSystems.getDefault().newWatchService()

      watchServiceRef.set(watchService)
      registerKeys(watchService)

      active = true

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
                     true
                  } else {
                     !path.fileName.toString().contains(".git") &&
                        path.fileName.toString().endsWith(".taxi")
                  }
               }
               .forEach { path -> emitter.onNext(RecompileRequestedSignal(path)) }
            key.reset()
         }
      } catch (e: ClosedWatchServiceException) {
         logger.warn(e) {"Watch service was closed. ${e.message}" }
      } catch (e: Exception) {
         logger.error(e) { "Error in watch service: ${e.message}" }
      }
   }
}
