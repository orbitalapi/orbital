package io.vyne.schemaServer.core.git

import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
class GitSyncTask(
   private val repositories: List<GitRepositorySourceLoader>
) {

   private val logger = KotlinLogging.logger {}


   //   @Scheduled(fixedRateString = "\${vyne.schemaServer.git.pollFrequency:PT30S}")
   fun sync() {
      repositories.forEach { gitRepository ->
         try {
            syncRepository(gitRepository)
         } catch (e: Exception) {
            logger.error(e) { "Failed to complete sync task on repository ${gitRepository.description}" }
         }
      }
   }

   private fun syncRepository(gitRepository: GitRepositorySourceLoader) {
      logger.info("Synchronizing repository: ${gitRepository.description}")
      val recompilationRequired = gitRepository.use { gitRepoResource ->
         if (gitRepoResource.lsRemote() == OperationResult.FAILURE) {
            logger.error("Sync error: Could not reach repository ${gitRepository.description}")
         }

         val operationResultedInChanges = gitRepository.fetchLatest()
         if (operationResultedInChanges) {
            gitRepository.emitSourcesChangedMessage()
         }

         operationResultedInChanges
      }
      // let recompilation happen through the emit messages on checkout and pull
//      if (recompilationRequired) {
//         logger.info { "Poll operation resulted in changes to repository ${gitRepository.description}.  Recompiling" }
//         sourceWatchingSchemaPublisher.refreshSources(gitRepository.sourceLoader)
//      } else {
//         logger.info { "Poll completed on ${gitRepository.description}, no work required." }
//      }
   }
}
