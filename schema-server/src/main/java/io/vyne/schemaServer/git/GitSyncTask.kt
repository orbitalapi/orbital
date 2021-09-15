package io.vyne.schemaServer.git

import io.vyne.schemaServer.FileWatcher
import io.vyne.schemaServer.LocalFileSchemaPublisherBridge
import io.vyne.schemaServer.VersionedSourceLoader
import mu.KotlinLogging
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
   private val fileSystemVersionedSourceLoader: VersionedSourceLoader,
   private val compilerService: CompilerService,
//   private val localFileSchemaPublisherBridge: LocalFileSchemaPublisherBridge
) {

   private val logger = KotlinLogging.logger {}


   private val inProgress = AtomicBoolean(false)

   init {
      logger.info("Git sync job created: \n$gitSchemaRepoConfig")
   }

   @Scheduled(fixedRateString = "\${taxi.gitCloningJobPeriodMs:300000}")
   fun sync() {
      if (inProgress.get()) {
         logger.warn("Another cloning process is running, exiting.")
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
            logger.info("Synchronizing repository: ${repoConfig.name} - ${repoConfig.uri} / ${repoConfig.branch}")
            val git = gitRepoProvider.provideRepo(rootDir.absolutePath, repoConfig)

            try {
               git.use { gitRepo ->
                  if (gitRepo.lsRemote() == OperationResult.FAILURE) {
                     logger.error("Synch error: Could not reach repository ${repoConfig.name} - ${repoConfig.uri} / ${repoConfig.branch}")
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
                  val sources = fileSystemVersionedSourceLoader.loadVersionedSources(incrementVersion = false)
                  compilerService.recompile(fileSystemVersionedSourceLoader.identifier, sources)
//                  localFileSchemaPublisherBridge.rebuildSourceList()
               }
            } catch (e: Exception) {
               logger.error("Synch error: ${repoConfig.name}\n${e.message}", e)
            }
         }
      } catch (e: Exception) {
         logger.error("Synch error", e)
      } finally {
         fileWatcher.watch()
         inProgress.set(false)
      }
   }
}
