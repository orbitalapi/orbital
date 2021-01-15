package io.vyne.schemaServer

import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
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
class FileWatcher(@Value("\${taxi.schema-local-storage}") private val schemaLocalStorage: String,
                  @Value("\${taxi.schema-recompile-interval-seconds:3}") private val schemaRecompileIntervalSeconds: Long,
                  @Value("\${taxi.schema-increment-version-on-recompile:true}") private val incrementVersionOnRecompile: Boolean,

                  private val compilerService: CompilerService,
                  private val excludedDirectoryNames: List<String> = FileWatcher.excludedDirectoryNames) {

   @Volatile
   var recompile: Boolean = false
   val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

   @PostConstruct
   fun init() {
      log().info("Creating FileWatcher")
      log().info("""taxi.schema-local-storage=${schemaLocalStorage}
| taxi.schema-recompile-interval-seconds=${schemaRecompileIntervalSeconds}
| taxi.schema-increment-version-on-recompile=${incrementVersionOnRecompile}""".trimMargin())
      // scheduling recompilations at fixed interval
      // this is useful to batch multiple taxi file updates into a single update
      scheduler.scheduleAtFixedRate({
         if (recompile) {
            recompile = false
            try {
               compilerService.recompile(incrementVersionOnRecompile)
            } catch (exception: Exception) {
               log().error("Exception in compiler service:", exception)
            }
         }
      }, 0, schemaRecompileIntervalSeconds, TimeUnit.SECONDS) // TODO move 3 sec to config
   }

   @PreDestroy
   fun destroy() {
      unregisterKeys()
      scheduler.shutdown()
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
      val path: Path = Paths.get(schemaLocalStorage)

      path.toFile().walkTopDown()
         .onEnter {
            val excluded = (it.isDirectory && excludedDirectoryNames.contains(it.name))
            !excluded
         }
         .filter { it.isDirectory }
         .forEach { directory ->
            registeredKeys += directory.toPath().register(
               watchService,
               StandardWatchEventKinds.ENTRY_CREATE,
               StandardWatchEventKinds.ENTRY_DELETE,
               StandardWatchEventKinds.ENTRY_MODIFY,
               StandardWatchEventKinds.OVERFLOW)
         }
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
      if (schemaLocalStorage.isNullOrEmpty()) {
         log().warn("schema-local-storage parameter in config file is empty, skipping.")
         return
      }

      log().info("Starting to watch $schemaLocalStorage")
      val watchService = FileSystems.getDefault().newWatchService()

      watchServiceRef.set(watchService)
      registerKeys(watchService)

      var key: WatchKey
      try {
         while (watchService.take().also { key = it } != null) {
            val events = key.pollEvents().filter {
               !(it.context() as Path).fileName.toString().contains(".git") &&
               (it.context() as Path).fileName.toString().contains(".taxi")
            }

            if (events.isNotEmpty()) {
               log().info("File change detected ${events.joinToString { "${it.kind()} ${it.context()}" }}")
               recompile = true
            }
            key.reset()
         }
      } catch (e: ClosedWatchServiceException) {
         log().info("Watch service was closed.  This could be because a git sync is about to start. ${e.message.orEmpty()}")
      } catch (e: Exception) {
         log().error("Error in watch service: ${e.message}")
      }
   }
}
