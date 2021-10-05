package io.vyne.schemaServer.file

import mu.KotlinLogging
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger {}

@Component
class FileSystemMonitorLifecycleHandler(val watchers: List<FileSystemMonitor>) {

   @PreDestroy
   fun onShutdown() {
      logger.info { "Shutting down.  Terminating file system watchers" }
      watchers.forEach {
         logger.info { "Stopping FileSystemMonitor at ${it.repository.projectPath}" }
         it.stop()
      }
   }

   @PostConstruct
   fun onStartup() {
      watchers.forEach {
         logger.info { "Starting FileSystemMonitor at ${it.repository.projectPath}" }
         it.start()
      }
   }
}
