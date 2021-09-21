package io.vyne.schemaServer.file

import io.vyne.schemaServer.editor.ApiEditorRepository
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.convert.DurationUnit
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Spring config class that's responsible wiring the file source loader
 * and file system watchers for each configured source directory
 *
 */
@Configuration
class FileWatcherBuilders {

   private val logger = KotlinLogging.logger {}

   @Bean
   fun buildFileWatchers(
      config: FileSchemaConfig?,
      repositories: List<FileSystemSchemaRepository>?,
   ): List<FileSystemMonitor> {
      if (config == null || repositories == null || repositories.isEmpty()) {
         return emptyList()
      }
      val paths = (config.paths + config.apiEditorProjectPath).filterNotNull().distinct()
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
   fun buildApiEditorRepository(
      config: FileSchemaConfig?,
      repositories: List<FileSystemSchemaRepository>?
   ): ApiEditorRepository? {
      if (config?.apiEditorProjectPath == null) {
         logger.info { "No apiEditorProjectPath is defined, so edits via the REST API are disabled" }
         return null
      }
      if (repositories == null) {
         logger.info { "No repositories are defined, so edits via the REST API are disabled" }
         return null
      }
      // By ensuring the apiEditorProjectPath is also a configured project path, it means that a watcher
      // has already been set up, and we don't have to do much work
      if (repositories.none { it.projectPath == config.apiEditorProjectPath }) {
         logger.error { "apiEditorProjectPath has been defined, but it's path is not in the list of project paths.  Add the apiEditorProjectPath to the list of configured paths.  Edits via the REST API are disabled" }
         return null
      }
      val repository = repositories.first { it.projectPath == config.apiEditorProjectPath }
      logger.info { "Project at path ${repository.projectPath.toAbsolutePath()} is configured to accept changes from the REST API" }
      return ApiEditorRepository(repository)
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
               path,
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
   val paths: List<Path> = emptyList(),
   val apiEditorProjectPath: Path?
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
