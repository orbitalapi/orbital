package io.vyne.schemaServer

import io.vyne.utils.log
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Component
class GitSynch(
   private val gitSchemaRepoConfig: GitSchemaRepoConfig,
   private val gitRepoProvider: GitRepoProvider,
   private val fileWatcher: FileWatcher,
   private val compilerService: CompilerService) {

   private var inProgress = AtomicBoolean(false)

   fun isInProgress(): Boolean {
      return inProgress.get()
   }

   fun synch() {
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
               git.use { it ->
                  if(it.lsRemote() == OperationResult.FAILURE) {
                     log().error("Synch error: Could not reach repository ${repoConfig.name} - ${repoConfig.uri} / ${repoConfig.branch}")
                     return@forEach
                  }

                  if(it.existsLocally()) {
                     it.checkout()
                     it.pull()
                  } else {
                     it.clone()
                     it.checkout()
                  }

                  if(it.isUpdated()) {
                     recompile = true
                  }
               }

               if(recompile) {
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
