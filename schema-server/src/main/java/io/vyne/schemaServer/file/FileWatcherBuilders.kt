package io.vyne.schemaServer.file

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * Spring config class that's responsible wiring the file source loader
 * and file system watchers for each configured source directory
 *
 */
@Configuration
class FileWatcherBuilders {

   @Suppress("SpringJavaInjectionPointsAutowiringInspection")
   @Autowired
   @Value("\${taxi.change-detection-method:watch}")
   lateinit var fileChangeDetectionMethod: FileChangeDetectionMethod

}


enum class FileChangeDetectionMethod {
   /**
    * Registers a FileSystem change watcher to be notified of
    * changes.
    *
    * The preferred approach, but does have some issues on Windows
    * systems, especially when running from a docker host (ie.,
    * a docker container running linux, watching a file system
    * that is mounted externally from a windows host).
    */
   WATCH,

   /**
    * Polls the file system periodically for changes
    */
   POLL
}
