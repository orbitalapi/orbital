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

   private val inProgress = AtomicBoolean(false)

   init {
      log().info("Git sync job created: \n$gitSchemaRepoConfig")
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
         val rootDir = File(gitSchemaRepoConfig.schemaLocalStorage!!)
         var recompile = false

         if (!rootDir.exists()) {
            rootDir.mkdir()
         }

         gitSchemaRepoConfig.gitSchemaRepos.forEach { repoConfig ->
            log().info("Synchronizing repository: ${repoConfig.name} - ${repoConfig.uri} / ${repoConfig.branch}")
            val git = gitRepoProvider.provideRepo(rootDir.absolutePath, repoConfig)

            try {
               git.use { gitRepo ->
                  if (gitRepo.lsRemote() == OperationResult.FAILURE) {
                     log().error("Synch error: Could not reach repository ${repoConfig.name} - ${repoConfig.uri} / ${repoConfig.branch}")
                     return@forEach
                  }

                  if (gitRepo.existsLocally()) {
                     gitRepo.checkout()
                     gitRepo.pull()
                  } else {
                     gitRepo.clone()
                     gitRepo.checkout()
                     recompile = true
                  }

                  if (gitRepo.isUpdated()) {
                     recompile = true
                  }
               }

               if (recompile) {
                  compilerService.recompile(false)
               }
            } catch (e: Exception) {
               log().error("Synch error: ${repoConfig.name}\n${e.message}", e)
            }
         }
      } catch (e: Exception) {
         log().error("Synch error", e)
      } finally {
         fileWatcher.watch()
         inProgress.set(false)
      }
   }
}
