package io.vyne.schemaServer

import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.nio.file.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Component
class FileWatcherInitializer(val watcher: FileWatcher) {

   @PostConstruct
   fun start() {
      watcher.watch()
   }

}

@Component
class FileWatcher(@Value("\${taxi.schema-local-storage}") private val schemaLocalStorage: String?,
                  @Value("\${taxi.schema-recompile-interval-seconds:3}") private val schemaRecompileIntervalSeconds: Long,
                  private val compilerService: CompilerService,
                  private val excludedDirectoryNames: List<String> = FileWatcher.excludedDirectoryNames) {

   @Volatile
   var recompile: Boolean = false
   val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

   @PostConstruct
   fun init() {
      log().info("taxi.schema-local-storage=${schemaLocalStorage} taxi.schema-recompile-interval-seconds=${schemaRecompileIntervalSeconds}")
      // scheduling recompilations at fixed interval
      // this is useful to batch multiple taxi file updates into a single update
      scheduler.scheduleAtFixedRate({
         if (recompile) {
            recompile = false
            try {
               compilerService.recompile()
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
      val path: Path = Paths.get(schemaLocalStorage!!)

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
         log().warn("Keys is closed. ${e.message}")
      } catch (e: Exception) {
         log().error("Error in watch service: ${e.message}")
      }
   }
}
