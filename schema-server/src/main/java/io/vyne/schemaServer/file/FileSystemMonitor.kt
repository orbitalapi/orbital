package io.vyne.schemaServer.file

import mu.KotlinLogging
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

interface FileSystemMonitor {
   fun start()
   fun stop()
}

private val logger = KotlinLogging.logger {}

@Component
class FileSystemMonitorLifecycleHandler(val watchers: List<FileSystemMonitor>) {

   @PreDestroy
   fun onShutdown() {
      logger.info { "Shutting down.  Terminating file system watchers" }
      watchers.forEach { it.stop() }
   }

   @PostConstruct
   fun onStartup() {
      logger.info { "Starting file system watchers" }
      watchers.forEach { it.start() }
   }
}
