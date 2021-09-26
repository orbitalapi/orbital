package io.vyne.schemaServer.file

import mu.KotlinLogging
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * FileSystemMonitors are responsible
 * for detecting changes on the file system (in a specific path), and
 * then notifying a provided FileSystemSchemaRepository to reload
 * its sources
 */
interface FileSystemMonitor {
   fun start()
   fun stop()
   val repository:FileSystemSchemaRepository
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
