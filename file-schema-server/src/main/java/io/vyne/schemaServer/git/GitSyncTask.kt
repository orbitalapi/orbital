package io.vyne.schemaServer.git

import io.vyne.schemaServer.CompilerService
import io.vyne.schemaServer.FileWatcher
import io.vyne.utils.log
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@ConditionalOnProperty(
   name = ["taxi.gitCloningJobEnabled"],
   havingValue = "true",
   matchIfMissing = false
)
@Component
class GitSyncTask(
   private val gitSchemaRepoConfig: GitSchemaRepoConfig,
   private val gitRepoProvider: GitRepoProvider,
   private val fileWatcher: FileWatcher,
   private val compilerService: CompilerService) {

   private val rootDir: File
   private val inProgress = AtomicBoolean(false)

   init {
      log().info("Git sync job created: \n$gitSchemaRepoConfig")
      if (gitSchemaRepoConfig.schemaLocalStorage != null) {
         rootDir = File(gitSchemaRepoConfig.schemaLocalStorage)
      } else {
         error("taxi.gitCloningJobEnabled is set to true, but no schema storage location was provided.  Set taxi.schemaLocalStorage")
      }

   }

   @Scheduled(fixedRateString = "\${taxi.gitCloningJobPeriodMs:300000}")
   fun sync() {
      if (inProgress.get()) {
         log().warn("Another cloning process is running, exiting.")
         return
      }

      inProgress.set(true)
      fileWatcher.cancelWatch()

      try {
         if (!rootDir.exists()) {
            rootDir.mkdir()
         }

         val syncTaskResults = gitSchemaRepoConfig.gitSchemaRepositories.map { repoConfig ->
            log().info("Synchronizing repository: ${repoConfig.name} - ${repoConfig.uri} / ${repoConfig.branch}")
            val git = gitRepoProvider.provideRepo(rootDir.toPath(), repoConfig)

            syncRepository(git, repoConfig)
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

   private fun syncRepository(git: GitRepo, repoConfig: GitRemoteRepository): OperationResult {
      val result = try {
         git.use { gitRepo ->
            if (gitRepo.lsRemote() == OperationResult.FAILED) {
               log().error("Sync error: Could not reach repository ${repoConfig.name} - ${repoConfig.uri} / ${repoConfig.branch}")
               return OperationResult.FAILED
            }

            val operationResult = if (gitRepo.existsLocally()) {
               gitRepo.checkout()
               gitRepo.pull()
            } else {
               gitRepo.clone()
               gitRepo.checkout()
            }
            operationResult
         }
      } catch (e: Exception) {
         log().error("Sync error: ${repoConfig.name}\n${e.message}", e)
         OperationResult.FAILED
      }
      return result
   }
}
