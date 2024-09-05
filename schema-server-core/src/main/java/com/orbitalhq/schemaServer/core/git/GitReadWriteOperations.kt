package com.orbitalhq.schemaServer.core.git

import com.orbitalhq.schema.publisher.loaders.ChangesetOverview
import com.orbitalhq.schemaServer.core.git.providers.GitHostingProviderRegistry
import mu.KotlinLogging
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.File
import java.util.*

/**
 * Provides access to git repositories to encapsulate writing tasks,
 * such as committing, raising a PR, branching, etc.
 */
class GitReadWriteOperations(
    workingDir: File,
    private val config: GitProjectSpec,
    private val hostingProviderRegistry: GitHostingProviderRegistry = GitHostingProviderRegistry()
) : GitPollOperations(workingDir, config) {

   companion object {
      private val logger = KotlinLogging.logger {}
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
