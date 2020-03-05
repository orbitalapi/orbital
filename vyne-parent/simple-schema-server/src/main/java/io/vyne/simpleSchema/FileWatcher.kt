package io.vyne.simpleSchema

import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.nio.file.*
import javax.annotation.PostConstruct


@Component
class FileWatcherInitializer(val watcher:FileWatcher) {

   @PostConstruct
   fun start() {
      watcher.watch()
   }

}

@Component
class FileWatcher(@Value("\${taxi.project-home}") val projectHome:String,
                  val compilerService: CompilerService)  {

   @Async
   fun watch() {
      log().info("Starting to watch $projectHome")
      val watchService = FileSystems.getDefault().newWatchService()
      val path: Path = Paths.get(projectHome)

      path.register(
         watchService,
         StandardWatchEventKinds.ENTRY_CREATE,
         StandardWatchEventKinds.ENTRY_DELETE,
         StandardWatchEventKinds.ENTRY_MODIFY)

      var key: WatchKey
      while (watchService.take().also { key = it } != null) {
         val events = key.pollEvents()
         if (events.isNotEmpty()) {
            log().info("File change detected - ${events.joinToString { "${it.kind()} ${it.context()}" }}")
            compilerService.recompile()
         }
         key.reset()
      }
   }
}
