package io.vyne.schemaServer.file

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.convert.DurationUnit
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Spring config class that's responsible wiring the file source loader
 * and file system watchers for each configured source directory
 *
 */
@Configuration
class FileWatcherBuilders {

   @Bean
   fun buildFileWatchers(
      config: FileSchemaConfig?,
      repositories: List<FileSystemSchemaRepository>?
   ): List<FileSystemMonitor> {
      if (config == null || repositories == null || repositories.isEmpty()) {
         return emptyList()
      }
      val watchers = when (config.changeDetectionMethod) {
         FileChangeDetectionMethod.POLL -> repositories.map { repository ->
            FilePoller(repository, config.pollFrequency)
         }
         FileChangeDetectionMethod.WATCH -> repositories.map { repository ->
            FileWatcher(repository, config.recompilationFrequencyMillis)
         }
      }
      return watchers
   }

   @Bean
   fun buildFileRepositories(
      config: FileSchemaConfig?
   ): List<FileSystemSchemaRepository> {
      @Suppress("IfThenToElvis")
      return if (config == null) {
         return emptyList()
      } else {
         config.paths.map { path ->
            FileSystemSchemaRepository.forPath(
               Paths.get(path),
               config.incrementVersionOnChange
            )
         }
      }
   }

}


@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.schema-server.file")
data class FileSchemaConfig(
   val changeDetectionMethod: FileChangeDetectionMethod = FileChangeDetectionMethod.WATCH,
   val pollFrequency: Duration = Duration.ofSeconds(5L),
   @DurationUnit(ChronoUnit.MILLIS)
   val recompilationFrequencyMillis: Duration = Duration.ofMillis(3000L),
   val incrementVersionOnChange: Boolean = false,
   val paths: List<String> = emptyList()
)

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
