package io.vyne.schemaServer

import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.util.FS
import java.io.File

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

class GitRepo(rootDir: String, gitRepoConfig: GitSchemaRepoConfig.GitRemoteRepo) : AutoCloseable {
   private val config: GitSchemaRepoConfig.GitRemoteRepo = gitRepoConfig
   private val workingDir: File
   private val gitDir: File
   private val repoBuilder: FileRepositoryBuilder = FileRepositoryBuilder()
   private val transportConfigCallback: TransportConfigCallback?
   private val repo: Repository
   private val gitRepo: Git
   private var updated: Boolean

   init {
      transportConfigCallback = if (!gitRepoConfig.sshPrivateKeyPath.isNullOrEmpty()) {
         SshTransportConfigCallback(gitRepoConfig.sshPrivateKeyPath, gitRepoConfig.sshPassPhrase)
      } else {
         null
      }
      workingDir = File("${rootDir}${File.separator}${gitRepoConfig.name}")
      gitDir = File("${workingDir}${File.separator}.git")
      repoBuilder.gitDir = gitDir
      repo = repoBuilder.build()
      gitRepo = Git.wrap(repo)
      updated = false
   }

   override fun close() {
      repo.close()
      gitRepo.close()
   }

   fun isUpdated(): Boolean {
      return updated
   }

   fun existsLocally(): Boolean {
      return repo.objectDatabase!!.exists()
   }

   fun clone(): OperationResult {
      CloneCommand()
         .setDirectory(workingDir)
         .setURI(config.uri)
         .setCloneAllBranches(true)
         .setTransportConfigCallback(transportConfigCallback)
         .call()
         .use {
            return OperationResult.fromBoolean(it.repository.objectDatabase!!.exists())
         }
   }

   fun pull(): OperationResult {
      val result = gitRepo.pull()
         .setRemoteBranchName(config.branch)
         .setTransportConfigCallback(transportConfigCallback)
         .call()

      if(result.fetchResult.trackingRefUpdates.isNotEmpty()) {
         updated = true
      }

      return OperationResult.fromBoolean(result.isSuccessful)
   }

   fun checkout() {
      val createBranch =
         !gitRepo
            .branchList()
            .call()
            .map { it.name }
            .contains("refs/heads/${config.branch}")

      gitRepo.checkout()
         .setCreateBranch(createBranch)
         .setStartPoint("origin/${config.branch}")
         .setName(config.branch)
         .call()
   }

   fun lsRemote(): OperationResult {
      val result = gitRepo.lsRemote()
         .setRemote(config.uri)
         .setTransportConfigCallback(transportConfigCallback)
         .call()

      return OperationResult.fromBoolean(result.isNotEmpty())
   }

   fun isGitRepo(): Boolean {
      return RepositoryCache.FileKey.isGitRepository(gitDir, FS.DETECTED)
   }
}
