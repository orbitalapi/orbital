package com.orbitalhq.schemaServer.core.git

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
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
         logger.debug { "Starting a git sync for ${config.description}" }
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

   fun start(syncImmediately: Boolean = true): Flux<GitSyncStatus> {
      // A ticker which emits immediately, then on the gitPollFrequency thereafter
      val ticker = if (syncImmediately) {
         syncNow(workingDir, config)
         Flux.just(0L)
            .concatWith(Flux.interval(gitPollFrequency))
      } else {
         Flux.interval(gitPollFrequency)
      }.publishOn(Schedulers.boundedElastic())

      return ticker.map { _ ->
         syncNow(workingDir, config)
      }
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
