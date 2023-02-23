package io.vyne.schemaServer.core.git

import io.vyne.schema.publisher.loaders.ChangesetOverview
import io.vyne.schemaServer.core.git.providers.GitHostingProviderRegistry
import mu.KotlinLogging
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.File
import java.nio.file.Files
import java.util.*


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

class GitOperations(
   val workingDir: File,
   private val config: GitRepositoryConfig,
   private val hostingProviderRegistry: GitHostingProviderRegistry = GitHostingProviderRegistry()
) : AutoCloseable {
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

   companion object {
      private val logger = KotlinLogging.logger {}
      private fun String.objectNameToDisplayName() = this.split("/").last()
      data class TestConnectionResult(
         val successful: Boolean,
         val errorMessage: String?,
         val branchNames: List<String>?,
         val defaultBranch: String?
      ) {
         companion object {
            fun failed(error: String) = TestConnectionResult(
               false,
               error,
               null,
               null
            )
         }
      }

      fun testConnection(url: String): TestConnectionResult {
         val branches = try {
            Git.lsRemoteRepository()
               .setRemote(url)
               .callAsMap()
         } catch (e: TransportException) {
            val message = e.message ?: ""
            if (message.contains("Authentication is required but no CredentialsProvider has been registered")) {
               return TestConnectionResult.failed(
                  "Authentication failed",
               )
            }
            if (message.contains("not found: Not Found")) {
               return TestConnectionResult.failed("Could not connect to the remote repository")
            }
            logger.warn(e) { "Failed to connect to git repo at $url, but the exception has not been handled" }
            return TestConnectionResult.failed("An unknown error occurred")
         }
         val defaultBranchName = branches.get("HEAD")
            ?.target?.name?.objectNameToDisplayName()

         val branchNames = branches.keys.filter { it.startsWith("refs/heads/") }
            .map { it.objectNameToDisplayName() }

         return TestConnectionResult(
            true,
            null,
            branchNames,
            defaultBranchName
         )

      }
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
      // TODO : This should be a shallow clone.
      // We need to wait for jgit 6.5, due for release shortly (as of Feb 2023).
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=475615
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


   fun commitAndPush(message: String) {
      git.add().addFilepattern(".").call()
      val commitResult = git.commit().setMessage(message).setAllowEmpty(true).call()
      logger.info { "Committed ${commitResult.id.abbreviate(8).name()} on $description with message $message" }
      pushToRemote()
   }

   fun raisePr(branchName: String, description: String, author: String): Pair<ChangesetOverview, String> {
      return hostingProviderRegistry.getService(config)
         .raisePr(config, branchName, description, author)
   }

   fun getChangesetOverview(branchName: String): ChangesetOverview {
      val oldTreeIterator = prepareTreeParser(branchName)
      val newTreeParser = prepareTreeParser(config.branch)
      if (oldTreeIterator == null || newTreeParser == null) {
         logger.error { "Failed to obtain the iterator for $branchName or ${config.branch}. Defaulting to an empty branch overview. " }
         return ChangesetOverview(0, 0, 0, "", "", Date())
      }
      var additions = 0
      var changedFiles = 0
      var deletions = 0
      git.diff().setOldTree(oldTreeIterator).setNewTree(newTreeParser).call().forEach { diffEntry ->
         when (diffEntry.changeType) {
            DiffEntry.ChangeType.ADD -> additions++
            DiffEntry.ChangeType.DELETE -> deletions++
            DiffEntry.ChangeType.MODIFY -> changedFiles++
            DiffEntry.ChangeType.COPY -> changedFiles++
            DiffEntry.ChangeType.RENAME -> changedFiles++
         }
      }

      // TODO
      val author = ""
      val description = ""
      val lastUpdated = git.log().setMaxCount(1).call().first().authorIdent.`when`
      return ChangesetOverview(additions, changedFiles, deletions, author, description, lastUpdated)
   }

   private fun prepareTreeParser(ref: String): AbstractTreeIterator? {
      val branchName = if (ref == config.branch) ref else "${config.pullRequestConfig!!.branchPrefix}/$ref"
      val head = git.repository.findRef("refs/heads/$branchName") ?: return null
      val walk = RevWalk(git.repository)
      val commit = walk.parseCommit(head.objectId)
      val tree = walk.parseTree(commit.tree.id)
      val oldTreeParser = CanonicalTreeParser()
      val oldReader = git.repository.newObjectReader()
      oldReader.use {
         oldTreeParser.reset(it, tree.id)
      }
      return oldTreeParser
   }

   // TODO Remove?
//   fun getPullRequestDetails(branchName: String): BranchOverview? {
//      return hostingProviderRegistry.getService(config)
//         .getPullRequestDetails(config, branchName)
//   }

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
         git.checkout()
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

   fun renameCurrentBranch(newName: String) {
      val prefixedName = withPrefix(newName)
      logger.info { "Renaming active branch to $prefixedName" }
      git.branchRename().setNewName(prefixedName).call()
      pushToRemote()
   }

   private fun pushToRemote() {
      logger.info { "Starting push to remote repo for $description" }
      val pushResult = git.push().setTransportConfigCallback(transportConfigCallback).call()
      logger.info { "Push completed to ${pushResult.joinToString { it.uri.toASCIIString() + " / " + it.remoteUpdates.joinToString { it.remoteName } }}" }

   }

   private fun withPrefix(name: String): String {
      return getCurrentPrefix() + name
   }

   private fun getCurrentPrefix(): String {
      return config.pullRequestConfig?.branchPrefix ?: ""
   }
}
