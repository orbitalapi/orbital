package io.vyne.schemaServerCore.file

import io.vyne.schemaServerCore.editor.ApiEditorRepository
import io.vyne.schemaServerCore.editor.DefaultApiEditorRepository
import io.vyne.schemaServerCore.editor.EditingDisabledRepository
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.time.Duration

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
      config: FileSystemSchemaRepositoryConfig?,
      repositories: List<FileSystemSchemaRepository>?,
   ): List<FileSystemMonitor> {
      if (config == null || repositories == null || repositories.isEmpty()) {
         return emptyList()
      }
      val paths = (config.paths + config.apiEditorProjectPath).filterNotNull().distinct()
      val watchers = when (config.changeDetectionMethod) {
         FileChangeDetectionMethod.POLL -> repositories.map { repository ->
            logger.info { "Configuring FilePoller at ${repository.projectPath}" }
            FilePoller(repository, config.pollFrequency)
         }
         FileChangeDetectionMethod.WATCH -> repositories.map { repository ->
            logger.info { "Configuring FileWatcher at ${repository.projectPath}" }
            FileWatcher(repository, config.recompilationFrequencyMillis)
         }
      }
      return watchers
   }

   @Bean
   fun buildApiEditorRepository(
      config: FileSystemSchemaRepositoryConfig?,
      repositories: List<FileSystemSchemaRepository>?
   ): ApiEditorRepository {
      if (config?.apiEditorProjectPath == null) {
         logger.info { "No apiEditorProjectPath is defined, so edits via the REST API are disabled" }
         return EditingDisabledRepository
      }
      if (repositories == null) {
         logger.info { "No repositories are defined, so edits via the REST API are disabled" }
         return EditingDisabledRepository
      }
      // By ensuring the apiEditorProjectPath is also a configured project path, it means that a watcher
      // has already been set up, and we don't have to do much work
      if (repositories.none { it.projectPath == config.apiEditorProjectPath }) {
         logger.error { "apiEditorProjectPath has been defined, but it's path is not in the list of project paths.  Add the apiEditorProjectPath to the list of configured paths.  Edits via the REST API are disabled" }
         return EditingDisabledRepository
      }
      val repository = repositories.first { it.projectPath == config.apiEditorProjectPath }
      logger.info { "Project at path ${repository.projectPath.toAbsolutePath()} is configured to accept changes from the REST API" }
      return DefaultApiEditorRepository(repository)
   }

   @Bean
   fun buildFileRepositories(
      config: FileSystemSchemaRepositoryConfig?
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


data class FileSystemSchemaRepositoryConfig(
   val changeDetectionMethod: FileChangeDetectionMethod = FileChangeDetectionMethod.WATCH,
   val pollFrequency: Duration = Duration.ofSeconds(5L),
   val recompilationFrequencyMillis: Duration = Duration.ofMillis(3000L),
   val incrementVersionOnChange: Boolean = false,
   val paths: List<Path> = emptyList(),
   val apiEditorProjectPath: Path? = null
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
