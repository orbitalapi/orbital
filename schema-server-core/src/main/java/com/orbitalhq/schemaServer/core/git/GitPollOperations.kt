package com.orbitalhq.schemaServer.core.git

import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Provides a wrapper around polling / fetching / pulling from a git repository
 */
open class GitPollOperations(
    val workingDir: File,
    private val config: GitRepositoryConnectionConfig,

    ) : AutoCloseable {
   protected val gitDir: File = workingDir.resolve(".git")

   @Suppress("JoinDeclarationAndAssignment")
   protected val transportConfigCallback: TransportConfigCallback?
   protected val fileRepository: Repository
   protected val git: Git
   val name: String = config.name
   val description = config.description


   init {
      transportConfigCallback = if (config.sshAuth != null) {
          SshTransportConfigCallback(config.sshAuth!!)
      } else if (config.credentials != null) {
          CredentialsTransportConfigCallback(config.credentials!!)
      } else {
         null
      }
      val repoBuilder = FileRepositoryBuilder()
      repoBuilder.gitDir = gitDir
      fileRepository = repoBuilder.build()
      git = Git.wrap(fileRepository)
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }


   override fun close() {
      fileRepository.close()
      git.close()
   }


   fun existsLocally(): Boolean {
      return fileRepository.objectDatabase!!.exists()
   }

   /**
    * Pulls or clones, depending on whether the resource already exists locally.
    * Returns a boolean indicating if changes made locally as a result of the fetch
    */
   fun fetchLatest(): GitSyncStatus {
      return try {
         if (existsLocally()) {
            checkout()
            val pullResult = pull()
            GitSyncStatus(
               successful = true,
               pulledChanges = pullResult.fetchResult.trackingRefUpdates.isNotEmpty(),
               hasUnresolvedMerges = pullResult.mergeResult?.mergedCommits?.isNotEmpty() ?: false,
               repository = config,
               checkoutRoot = workingDir.toPath()
            )
         } else {
            val workingDirPath = workingDir.toPath()
            if (!Files.exists(workingDirPath.parent)) {
               Files.createDirectories(workingDirPath.parent)
            }
            clone()
            checkout()
            GitSyncStatus(
               successful = true,
               pulledChanges = true,
               hasUnresolvedMerges = false,
               repository = config,
               checkoutRoot = workingDir.toPath()
            )
         }
      } catch (e:Exception) {
         val errorMessage = "Failed to perform git sync to config ${config.name} - ${e::class.simpleName} - ${e.message}"
         logger.warn { errorMessage }
         GitSyncStatus(
            successful = false,
            pulledChanges = false,
            hasUnresolvedMerges = false,
            repository = config,
            checkoutRoot = workingDir.toPath(),
            errorMessage = errorMessage
         )
      }
   }

   fun clone(): OperationResult {
      // TODO : This should be a shallow clone.
      // We need to wait for jgit 6.5, due for release shortly (as of Feb 2023).
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=475615
      val refBranchName = "refs/heads/" + config.branch
      Git.cloneRepository()
         .setDirectory(workingDir)
         .setURI(config.uri)
         .setBranchesToClone(setOf(refBranchName))
         .setTransportConfigCallback(transportConfigCallback)
         .call()
         .use {
            return OperationResult.fromBoolean(it.repository.objectDatabase!!.exists())
         }
   }

   private fun pull(): PullResult {
      val pullResult = git.pull()
         .setRemoteBranchName(config.branch)
         .setTransportConfigCallback(transportConfigCallback)
         .setFastForward(MergeCommand.FastForwardMode.FF)
         .setRebase(true)
         .setStrategy(MergeStrategy.THEIRS)
         .call()

      return pullResult
   }

   fun checkout() {
      val createBranch =
         !git
            .branchList()
            .call()
            .map { it.name }
            .contains("refs/heads/${config.branch}")

      git.checkout()
         .setCreateBranch(createBranch)
         .setStartPoint("origin/${config.branch}")
         .setName(config.branch)
         .call()
   }
}

data class GitSyncStatus(
   val successful: Boolean,
   val pulledChanges: Boolean,
   val hasUnresolvedMerges: Boolean,
   val repository: GitRepositoryConnectionConfig,
   val checkoutRoot: Path,
   val errorMessage: String? = null
) {
   val description: String
      get() {

         val stateMessage = if (successful) {
            "success"
         } else {
            "failed: $errorMessage"
         }
         return "Git sync for ${repository.description} $stateMessage"
      }
}
