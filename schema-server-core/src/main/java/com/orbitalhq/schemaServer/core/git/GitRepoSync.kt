package com.orbitalhq.schemaServer.core.git

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.nio.file.Path
import java.time.Duration

/**
 * Creates a flux for polling and cloning a git repo at a specified duration
 */
class GitRepoSync(
   private val workingDir: Path,
   private val config: GitRepositoryConnectionConfig,
   private val gitPollFrequency: Duration = Duration.ofSeconds(30),
) {
   companion object {
      private val logger = KotlinLogging.logger {}

       fun syncNow(workingDir: Path, config: GitRepositoryConnectionConfig): GitSyncStatus {
         logger.info { "Starting a git sync for ${config.description}" }
         val result = try {
            GitPollOperations(workingDir.normalize().toFile(), config).fetchLatest()
         } catch (e: Exception) {
            GitSyncStatus(
               successful = false,
               pulledChanges = false,
               hasUnresolvedMerges = false,
               repository = config,
               checkoutRoot = workingDir,
               errorMessage = e.message
            )
         }
         if (result.successful) {
            logger.debug { result.description }
         } else {
            logger.warn { result.description }
         }

         return result
      }
   }

   fun syncNow(): GitSyncStatus {
      return syncNow(workingDir, config)
   }

   fun start(syncImmediately: Boolean = true, beforeGitOp: () -> Unit = {}, afterGitOp: () -> Unit = {}): Flux<GitSyncStatus> {
      logger.info { "Starting sync poll for ${config.description} with frequency $gitPollFrequency" }
      // A ticker which emits immediately, then on the gitPollFrequency thereafter
      val ticker = if (syncImmediately) {
         Flux.interval(Duration.ZERO, gitPollFrequency)
      } else {
         Flux.interval(gitPollFrequency)
      }
         //.publishOn(Schedulers.boundedElastic())

      return ticker.map { tick ->
         logger.debug { "tick => git sync internal for ${config.description}, tick => $tick" }
         syncNowInternal(workingDir, config, beforeGitOp, afterGitOp)
      }
   }

   private fun syncNowInternal(
      workingDir: Path,
      config: GitRepositoryConnectionConfig,
      beforeGitOp: () -> Unit,
      afterGitOp: () -> Unit): GitSyncStatus {
      logger.debug { "Starting a git sync internal for ${config.description}" }
      val result = try {
         beforeGitOp()
         GitPollOperations(workingDir.normalize().toFile(), config).fetchLatest()
      } catch (e: Exception) {
         GitSyncStatus(
            successful = false,
            pulledChanges = false,
            hasUnresolvedMerges = false,
            repository = config,
            checkoutRoot = workingDir,
            errorMessage = e.message
         )
      } finally {
          afterGitOp()
      }
      if (result.successful) {
         logger.debug { result.description }
      } else {
         logger.warn { result.description }
      }

      return result
   }


}



interface GitRepositoryConnectionConfig {
   val name: String

   @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   val uri: String
   val branch: String

   @get:JsonIgnore
   val sshAuth: GitSshAuth?

   @get:JsonIgnore
   val credentials: GitCredentials?


   val description: String
      get() {
         return "$name - $redactedUrl / $branch"
      }

   val redactedUrl: String
      get() {
         return GitProjectStoreSpec.redactUrl(this.uri)
      }
}
