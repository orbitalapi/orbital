package io.vyne.schemaServer.core.git

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.vyne.schemaServer.core.git.providers.GitHostingProviderRegistry
import mu.KotlinLogging
import org.eclipse.jgit.api.*
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Files


enum class OperationResult {
   SUCCESS, FAILURE;

   companion object {
      fun fromBoolean(input: Boolean): OperationResult {
         return if (input) {
            SUCCESS
         } else {
            FAILURE
         }
      }
   }
}

class GitOperations(val workingDir: File, private val config: GitRepositoryConfig, private val hostingProviderRegistry: GitHostingProviderRegistry = GitHostingProviderRegistry()) : AutoCloseable {
   private val gitDir: File = workingDir.resolve(".git")

   @Suppress("JoinDeclarationAndAssignment")
   private val transportConfigCallback: TransportConfigCallback?
   private val fileRepository: Repository
   private val git: Git
   val name: String = config.name
   val description = config.description

   private val logger = KotlinLogging.logger {}

   init {
      transportConfigCallback = if (config.sshAuth != null) {
         SshTransportConfigCallback(config.sshAuth)
      } else if (config.credentials != null) {
         CredentialsTransportConfigCallback(config.credentials)
      } else {
         null
      }
      val repoBuilder = FileRepositoryBuilder()
      repoBuilder.gitDir = gitDir
      fileRepository = repoBuilder.build()
      git = Git.wrap(fileRepository)
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
   fun fetchLatest(): Boolean {
      return if (existsLocally()) {
         checkout()
         val pullResult = pull()
         pullResult.mergeResult?.mergedCommits?.isNotEmpty() ?: false
      } else {
         val workingDirPath = workingDir.toPath()
         if (!Files.exists(workingDirPath.parent)) {
            Files.createDirectories(workingDirPath.parent)
         }
         clone()
         checkout()
         true
      }
   }

   fun clone(): OperationResult {
      Git.cloneRepository()
         .setDirectory(workingDir)
         .setURI(config.uri)
         .setCloneAllBranches(true)
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
         .setRebase(BranchConfig.BranchRebaseMode.REBASE)
         .call()

      // MP: 09-Aug-22: Dispatching of events now centralized to the GitSyncTask
//      if (pullResult.mergeResult.mergedCommits.isNotEmpty()) {
//         val sourcesChangedMessage = SourcesChangedMessage(this.loadVersionedSources())
//         this.sourcesChangedSink.emitNext(sourcesChangedMessage) { signalType, emitResult ->
//            logger.warn { "Pull operation for ${this.description} completed successfully, but failed to emit sources change message: $signalType $emitResult" }
//            false
//         }
//      }
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

   fun lsRemote(): OperationResult {
      val result = git.lsRemote()
         .setRemote(config.uri)
         .setTransportConfigCallback(transportConfigCallback)
         .call()

      return OperationResult.fromBoolean(result.isNotEmpty())
   }

   fun commitAndPush(message: String) {
      val addResult = git.add().addFilepattern(".").call()
      val commitResult = git.commit().setMessage(message).setAllowEmpty(true).call()
      logger.info { "Committed ${commitResult.id.abbreviate(8).name()} on $description with message $message" }
      logger.info { "Starting push to remote repo for $description" }
      val pushResult = git.push().setTransportConfigCallback(transportConfigCallback).call()
      logger.info { "Push completed to ${pushResult.joinToString { it.uri.toASCIIString() + " / " + it.remoteUpdates.joinToString { it.remoteName }}}" }
   }

   fun raisePr(branchName: String, description: String, author: String): String {
      return hostingProviderRegistry.getService(config)
         .raisePr(config, branchName, description, author)
   }

   /**
    * Creates a branch, optionally appending a prefix if so configured.
    * Pushes the branch to the remote.
    * The repository is switched to the newly created branch.
    * Returns the name of the created branch.
    */
   fun createBranch(branchName: String): String {
      val prefix = config.pullRequestConfig?.branchPrefix ?: ""
      return try {
         val prefixedBranchName = "$prefix$branchName"
         logger.info { "Creating and switching to branch $prefixedBranchName" }
         val result = git.checkout()
            .setCreateBranch(true)
            .setName(prefixedBranchName)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .call()
         logger.info { "Branch $prefixedBranchName created locally. Pushing to remote" }
         val pushResult = git.push().setTransportConfigCallback(transportConfigCallback).call()
         logger.info { "Push completed - the following refs were updated: ${pushResult.joinToString { it.remoteUpdates.joinToString { update -> update.remoteName } }}" }
         prefixedBranchName
      } catch (e: Exception) {
         logger.error(e) { "Failed to create branch $branchName. " }
         throw e
      }
   }

   fun getBranches(): List<String> {
      val mainBranch = config.branch
      if (config.pullRequestConfig == null) {
         return listOf(mainBranch)
      }
      val prefix = "refs/heads/" + config.pullRequestConfig.branchPrefix
      val availableBranches = git.branchList().call()
         .filter { it.name.startsWith(prefix) }
         .map { it.name.substringAfter(prefix) }

      return availableBranches + mainBranch
   }
}
