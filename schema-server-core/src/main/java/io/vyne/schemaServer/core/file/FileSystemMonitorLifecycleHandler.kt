package io.vyne.schemaServer.core.file

import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger {}

@Configuration
class FileSystemMonitorLifecycleHandlerConfiguration {
   @Bean
   fun fileSystemMonitorLifecycleHandler(watchers: List<FileSystemMonitor>) = FileSystemMonitorLifecycleHandler(watchers)

}

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
