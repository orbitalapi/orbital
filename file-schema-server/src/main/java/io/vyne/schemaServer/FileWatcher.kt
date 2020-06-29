package io.vyne.schemaServer

import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Component
import java.nio.file.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.concurrent.withLock


@Component
class FileWatcherInitializer(val watcher: FileWatcher) {

   @PostConstruct
   fun start() {
      watcher.watch()
   }

}

@Component
class FileWatcher(@Value("\${taxi.schema-local-storage}") val schemaLocalStorage: String?,
                  val compilerService: CompilerService,
                  val excludedDirectoryNames: List<String> = FileWatcher.excludedDirectoryNames) {

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
               try {
                  compilerService.recompile()
               } catch (exception: Exception) {
                  log().error("Exception in compiler service:", exception)
               }
            }
            key.reset()
         }
      } catch (e: Exception) {
         log().error("Error in watch service", e)
      }
   }
}
