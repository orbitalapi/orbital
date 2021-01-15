package io.vyne.schemaServer.git

import io.vyne.utils.log
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RemoteRefUpdate
import java.nio.file.Path

enum class OperationResult {
   NOT_ATTEMPTED,
   SUCCESS_WITHOUT_CHANGES,
   SUCCESS_WITH_CHANGES,
   ABORTED,
   FAILED;
}

class GitRepo(val workingDir: Path, private val config: GitRepositoryConfig) : AutoCloseable {

   val name: String = config.name
   val branch: String = config.branch
   val editable = config.editable
   val uri: String = config.uri

   companion object {
      fun asDirectoryInPath(root: Path, config: GitRepositoryConfig): GitRepo {
         return GitRepo(root.resolve(config.name + "/"), config)
      }
   }

   private val transportConfigCallback: TransportConfigCallback?
   private val git: Git
   private val repository: Repository

   init {
      transportConfigCallback = if (!config.sshPrivateKeyPath.isNullOrEmpty()) {
         SshTransportConfigCallback(config.sshPrivateKeyPath, config.sshPassPhrase)
      } else {
         null
      }
      repository = FileRepositoryBuilder()
         .setGitDir(workingDir.resolve(".git/").toFile())
         .readEnvironment()
         .build()
      git = Git.wrap(repository)
   }

   override fun close() {
      repository.close()
      git.close()
   }


   fun existsLocally(): Boolean {
      return repository.objectDatabase!!.exists()
   }

   fun isClean(): Boolean {
      log().info("Checking git status of local repository ${config.name} at $workingDir")
      val gitStatus = git.status().call()
      return gitStatus.isClean
   }

   fun clone(): OperationResult {
      log().info("Attempting to clone repository ${config.name} from ${config.uri} to $workingDir")
      CloneCommand()
         .setDirectory(workingDir.toFile())
         .setURI(config.uri)
         .setCloneAllBranches(true)
         .setTransportConfigCallback(transportConfigCallback)
         .call()
         .use { git ->
            return if (git.repository.objectDatabase.exists()) {
               log().info("Repository ${config.name} cloned successfully from ${config.uri} to $workingDir")
               OperationResult.SUCCESS_WITH_CHANGES
            } else {
               log().warn("Repository ${config.name} failed to clone from ${config.uri} to $workingDir")
               OperationResult.FAILED
            }
         }

   }

   fun pull(): OperationResult {
      log().info("Attempting to pull ${config.uri} on branch ${config.branch} in $workingDir")
      val result = git.pull()
         .setRemoteBranchName(config.branch)
         .setTransportConfigCallback(transportConfigCallback)
         .call()

      return when {
         !result.isSuccessful -> OperationResult.FAILED
         result.fetchResult.trackingRefUpdates.isNotEmpty() -> OperationResult.SUCCESS_WITH_CHANGES
         else -> OperationResult.SUCCESS_WITHOUT_CHANGES
      }
   }

   fun checkout(): OperationResult {
      val createBranch =
         !git
            .branchList()
            .call()
            .map { it.name }
            .contains("refs/heads/${config.branch}")

      log().info("Attempting to checkout branch ${config.branch} for repo ${config.name} checked out at $workingDir")
      git.checkout()
         .setCreateBranch(createBranch)
         .setStartPoint("origin/${config.branch}")
         .setName(config.branch)
         .call()
      log().info("Checkout of branch ${config.branch} successful")
      return OperationResult.SUCCESS_WITH_CHANGES
   }

   fun lsRemote(): OperationResult {
      val result = git.lsRemote()
         .setRemote(config.uri)
         .setTransportConfigCallback(transportConfigCallback)
         .call()

      return if (result.isNotEmpty()) {
         OperationResult.SUCCESS_WITHOUT_CHANGES
      } else {
         OperationResult.FAILED
      }
   }

   fun commitFile(path: Path, author: Author, commitMessage: String): CommitResult {

      val pathRelativeToWorkingDir = workingDir.relativize(path.toAbsolutePath())
      log().debug("Attempting to commit $path from $author with message $commitMessage")
      val addResult = git.add()
         .addFilepattern(pathRelativeToWorkingDir.toString())
         .call()


      val status = git.status().call()
      if (status.uncommittedChanges.size != 1) {
         log().warn(
            "After adding path $path there are ${status.uncommittedChanges.size} uncommitted changed, but expected to see 1:  ${
               status.uncommittedChanges.joinToString(
                  ","
               )
            }"
         )
      }
      // and then commit the changes
      val commitResult = git.commit()
         .setAuthor(author.toPersonIdent())
         .setMessage(commitMessage)
         .call()
      val repoSha = commitResult.toObjectId().abbreviate(8).name()
      val postCommitStatus = git.status().call()
      if (postCommitStatus.isClean) {
         log().info("After commit of path $path repo is clean on sha $repoSha")
      } else {
         log().warn("After commit of path $path repo is not clean, has ${postCommitStatus.uncommittedChanges.size} uncommitted changes, ${postCommitStatus.added.size} added changes, ${postCommitStatus.changed.size} changed changes, ${postCommitStatus.removed.size} remove changes and ${postCommitStatus.untracked.size} untracked files ")
      }

      return CommitResult(path, postCommitStatus, commitResult.toObjectId())

   }

   fun push(): OperationResult {
      val result = git.push()
         .call()
         .flatMap { pushResult ->
            pushResult.remoteUpdates
         }

      result.forEach { remoteUpdate ->
         if (remoteUpdate.status == RemoteRefUpdate.Status.OK) {
            log().info("Push to ${remoteUpdate.remoteName} completed with status ${remoteUpdate.status}")
         } else {
            log().warn("Push to ${remoteUpdate.remoteName} completed with status ${remoteUpdate.status} - ${remoteUpdate.message}")
         }
      }

      if (result.all { it.status == RemoteRefUpdate.Status.OK }) {
         return OperationResult.SUCCESS_WITHOUT_CHANGES
      } else {
         return OperationResult.FAILED
      }

   }

}

data class CommitResult(val path: Path, val gitStatus: Status, val sha: ObjectId) {
   val shortSha = sha.abbreviate(8).name()
}

data class Author(val name: String, val email: String) {

}

fun Author.toPersonIdent(): PersonIdent {
   return PersonIdent(this.name, this.email)
}


