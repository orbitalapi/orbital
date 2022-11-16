package io.vyne.schemaServer.core.git

import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.kohsuke.github.GitHub
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

class GitOperations(val workingDir: File, private val config: GitRepositoryConfig) : AutoCloseable {
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
      git.add().addFilepattern(".").call()
      git.commit().setMessage(message).setAllowEmpty(true).call()
      git.push().setTransportConfigCallback(transportConfigCallback).call()
   }

   fun raisePr(branchName: String, description: String, author: String): String {
      if (config.updateFlowConfig == null) {
         error("Don't know how to finalize changes for $branchName as there's no update flow config defined.")
      }
      // TODO Handle auth properly
      val github = GitHub.connectUsingPassword(config.credentials!!.username, config.credentials.password)
      val repo = github.getRepository(config.updateFlowConfig.repoName)
      val response = repo.createPullRequest(
         "Update $branchName",
         config.updateFlowConfig.branchPrefix + branchName,
         config.branch,
         "This PR was generated by Vyne as per a edit request by $author. Description of changes:\n\n$description"
      )
      return response.htmlUrl.toString()
   }

   fun createBranch(branchName: String): Boolean {
      val prefix = config.updateFlowConfig?.branchPrefix ?: ""
      return try {
         git.checkout().setCreateBranch(true).setName("$prefix$branchName").call()
         true
      } catch (e: Exception) {
         logger.error(e) { "Failed to create branch $branchName. " }
         false
      }
   }

   fun getBranches(): List<String> {
      if (config.updateFlowConfig == null) {
         return emptyList()
      }
      val prefix = "refs/heads/" + config.updateFlowConfig.branchPrefix
      val availableBranches = git.branchList().call()
         .filter { it.name.startsWith(prefix) }
         .map { it.name.substringAfter(prefix) }
      val mainBranch = config.branch
      return availableBranches + mainBranch
   }
}
