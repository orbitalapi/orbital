package io.vyne.schemaServer.git

import io.vyne.schemaServer.CompilerService
import io.vyne.schemaServer.FileWatcher
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@ConditionalOnProperty(
   name = ["taxi.git-sync-enabled"],
   havingValue = "true",
   matchIfMissing = false
)
@Component
class GitSyncTask(
   private val gitRepoProvider: GitRepoProvider,
   private val fileWatcher: FileWatcher,
   private val compilerService: CompilerService,
   @Value("\${taxi.git-sync-period-ms}") private val syncPeriodMs: Int = 10000
) {

   private val inProgress = AtomicBoolean(false)

   init {
      log().info("Git sync task is active with a sync period of ${syncPeriodMs}ms")
   }

   @Scheduled(fixedRateString = "\${taxi.git-sync-period-ms:300000}")
   fun sync() {
      if (inProgress.get()) {
         log().warn("Another cloning process is running, exiting.")
         return
      }

      inProgress.set(true)
      fileWatcher.cancelWatch()

      try {
         val syncTaskResults = gitRepoProvider.repositories.map { gitRepo ->
            log().info("Synchronizing repository: ${gitRepo.name} - ${gitRepo.uri} / ${gitRepo.branch}")
            syncRepository(gitRepo)
         }

         if (syncTaskResults.contains(OperationResult.SUCCESS_WITH_CHANGES)) {
            compilerService.recompile(incrementVersion = false)
         }
      } catch (e: Exception) {
         log().error("Sync error", e)
      } finally {
         fileWatcher.watch()
         inProgress.set(false)
      }
   }

   private fun syncRepository(gitRepo: GitRepo): OperationResult {
      val result = try {
         gitRepo.use {
            if (gitRepo.lsRemote() == OperationResult.FAILED) {
               log().error("Sync error: Could not reach repository ${gitRepo.name} - ${gitRepo.uri} / ${gitRepo.branch}")
               return OperationResult.FAILED
            }

            val operationResult = if (gitRepo.existsLocally()) {
               if (!gitRepo.isClean()) {
                  log().warn("Not syncing repository ${gitRepo.name} at ${gitRepo.workingDir} as the local repository is not clean.  Aborting")
                  OperationResult.ABORTED
               } else {
                  gitRepo.checkout()
                  gitRepo.pull()
               }

            } else {
               gitRepo.clone()
               gitRepo.checkout()
            }
            operationResult
         }
      } catch (e: Exception) {
         log().error("Sync error: ${gitRepo.name}\n${e.message}", e)
         OperationResult.FAILED
      }
      return result
   }
}
