package io.vyne.schemaServerCore.git

import io.vyne.VersionedSource
import io.vyne.schemaServerCore.UpdatingVersionedSourceLoader
import io.vyne.schemaServerCore.file.FileSystemVersionedSourceLoader
import io.vyne.schemaServerCore.file.SourcesChangedMessage
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.util.FS
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
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

class GitRepositorySourceLoader(val workingDir: File, private val config: GitRepositoryConfig) : AutoCloseable,
   UpdatingVersionedSourceLoader {
   private val gitDir: File = workingDir.resolve(".git")
   private val transportConfigCallback: TransportConfigCallback?
   private val fileRepository: Repository
   private val git: Git
   val sourceLoader: FileSystemVersionedSourceLoader =
      FileSystemVersionedSourceLoader.forProjectHome(workingDir.canonicalPath)

   val name:String = config.name
   val description = config.description

   private val logger = KotlinLogging.logger {}

   init {
      transportConfigCallback = if (!config.sshPrivateKeyPath.isNullOrEmpty()) {
         SshTransportConfigCallback(config.sshPrivateKeyPath, config.sshPassPhrase)
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

   fun pull(): PullResult {
      val pullResult = git.pull()
         .setRemoteBranchName(config.branch)
         .setTransportConfigCallback(transportConfigCallback)
         .call()

      if (pullResult.mergeResult.mergedCommits.isNotEmpty()) {
         val sourcesChangedMessage = SourcesChangedMessage(this.loadVersionedSources())
         this.sourcesChangedSink.emitNext(sourcesChangedMessage) { signalType, emitResult ->
            logger.warn { "Pull operation for ${this.description} completed successfully, but failed to emit sources change message: $signalType $emitResult" }
            false
         }
      }
      return pullResult
   }

   fun checkout(emitSourcesChangeMessage:Boolean = true) {
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

      if (emitSourcesChangeMessage) {
         val sourcesChangedMessage = SourcesChangedMessage(this.loadVersionedSources())
         this.sourcesChangedSink.emitNext(sourcesChangedMessage) { signalType, emitResult ->
            logger.warn { "Checkout operation for ${this.description} completed successfully, but failed to emit sources change message: $signalType $emitResult" }
            false
         }
      }
   }

   fun lsRemote(): OperationResult {
      val result = git.lsRemote()
         .setRemote(config.uri)
         .setTransportConfigCallback(transportConfigCallback)
         .call()

      return OperationResult.fromBoolean(result.isNotEmpty())
   }

   fun isGitRepo(): Boolean {
      return RepositoryCache.FileKey.isGitRepository(gitDir, FS.DETECTED)
   }

   private val sourcesChangedSink = Sinks.many().multicast().onBackpressureBuffer<SourcesChangedMessage>()
   override val sourcesChanged: Flux<SourcesChangedMessage>
      get() = sourcesChangedSink.asFlux()
   override val identifier: String = this.description

   override fun loadVersionedSources(forceVersionIncrement: Boolean, cachedValuePermissible: Boolean): List<VersionedSource> {
      return this.sourceLoader.loadVersionedSources(forceVersionIncrement)
   }
}
