package io.vyne.schemaServer.git

import io.vyne.schemaServer.publisher.SourceWatchingSchemaPublisher
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
class GitSyncTask(
   private val repositories: List<GitRepositorySourceLoader>,
   private val sourceWatchingSchemaPublisher: SourceWatchingSchemaPublisher
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
      val workingDirPath = gitRepository.workingDir.toPath()
      if (!Files.exists(workingDirPath.parent)) {
         Files.createDirectories(workingDirPath.parent)
      }

      logger.info("Synchronizing repository: ${gitRepository.description}")
      val recompilationRequired = gitRepository.use { gitRepoResource ->
         if (gitRepoResource.lsRemote() == OperationResult.FAILURE) {
            logger.error("Sync error: Could not reach repository ${gitRepository.description}")
         }

         val operationResultedInChanges = if (gitRepoResource.existsLocally()) {
            // Use checkout to ensure we're on the correct branch locally
            gitRepoResource.checkout(emitSourcesChangeMessage = false)
            val pullResult = gitRepoResource.pull()
            pullResult.mergeResult.mergedCommits.isNotEmpty()
         } else {
            val operationResult = gitRepoResource.clone()
            gitRepoResource.checkout(emitSourcesChangeMessage = true)
            true
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
