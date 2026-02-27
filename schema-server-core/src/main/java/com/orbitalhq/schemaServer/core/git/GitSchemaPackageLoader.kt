package com.orbitalhq.schemaServer.core.git

import com.orbitalhq.Message
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.ResultWithMessage
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigSourceWriter
import com.orbitalhq.schema.publisher.PublisherType
import com.orbitalhq.schema.publisher.loaders.AddChangesToChangesetResponse
import com.orbitalhq.schema.publisher.loaders.AvailableChangesetsResponse
import com.orbitalhq.schema.publisher.loaders.Changeset
import com.orbitalhq.schema.publisher.loaders.CreateChangesetResponse
import com.orbitalhq.schema.publisher.loaders.FinalizeChangesetResponse
import com.orbitalhq.schema.publisher.loaders.LoaderExposingTaxiProject
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schema.publisher.loaders.SetActiveChangesetResponse
import com.orbitalhq.schema.publisher.loaders.UpdateChangesetResponse
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageWriter
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import com.orbitalhq.utils.files.ReactiveFileSystemMonitor
import com.orbitalhq.utils.files.ReactiveWatchingFileSystemMonitor
import com.typesafe.config.Config
import kotlinx.coroutines.reactor.mono
import lang.taxi.messages.Severity
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.toPath

private val logger = KotlinLogging.logger { }

class GitSchemaPackageLoader(
   val workingDir: Path,
   override val config: GitProjectSpec,
   adaptor: SchemaSourcesAdaptor,
   // visible for testing
   val fileMonitor: ReactiveFileSystemMonitor = ReactiveWatchingFileSystemMonitor(workingDir, listOf(".git")),
   val gitPollFrequency: Duration = Duration.ofSeconds(30),
) : SchemaPackageTransport, LoaderExposingTaxiProject {

   override val publisherType: PublisherType = PublisherType.GitRepo

   override val description: String = "GitLoader at ${config.description}"

   val filePackageLoader: FileSystemPackageLoader

   private var currentBranch = config.branch
   private val defaultBranchName = config.branch

   private var isStopped = false

   private val gitStatusSink = Sinks.many().replay().latest<LoaderStatus>()
   override val loaderStatus: Flux<LoaderStatus>

   init {
      // This is invoked on `SingleThreadedSinkEmitter` not on the main Thread.
      GitRepoSync.syncNow(workingDir, config)
      val safePath = if (config.path.startsWith(FileSystems.getDefault().separator)) {
         Paths.get(".${FileSystems.getDefault().separator}" + config.path)
      } else {
         config.path
      }
      val pathWithGitRepo = workingDir.resolve(safePath).normalize()
      filePackageLoader = FileSystemPackageLoader(
         config = FileProjectSpec(
            pathWithGitRepo,
            config.loader,
         ),
         adaptor = adaptor,
         fileMonitor = fileMonitor,
         transportDecorator = this
      )
      gitStatusSink.emitNext(LoaderStatus.STARTING, Sinks.EmitFailureHandler.FAIL_FAST)
      val gitStatusMessages = gitStatusSink.asFlux()
         .distinctUntilChanged()
      val fileStatusMessages = filePackageLoader.loaderStatus
      loaderStatus = GitFileLoaderStatus.build(gitStatusMessages, fileStatusMessages)
         .doOnNext { status -> logger.info { "Git project loader at ${config.path} changed state: $status" } }
   }

   override fun toString(): String {
      return "${this::class.simpleName} for project ${config.description} at ${config.path}"
   }

   override fun loadNow(): Mono<SourcePackage> {
      // syncNow()
      return filePackageLoader.loadNow()
   }

   override fun stop() {
      logger.info { "Stopping git package loader configured for ${config.path}" }
      gitStatusSink.tryEmitComplete()
      filePackageLoader.stop()
      this.isStopped = true
   }


   override fun start(): Flux<SourcePackage> {
      logger.info { "Starting with workingDir => $workingDir" }
      return GitRepoSync(workingDir, config, gitPollFrequency)
         .start(true, { fileMonitor.suspend() }, { fileMonitor.resume() })
         .takeWhile { !isStopped }
         .doOnNext {
            updateLoaderStatus(it)
         }
         .index()
         .filter {
            // On first load: allow if sync succeeded OR if repo existed locally (previous sync available)
            // On subsequent ticks: only load if new changes were pulled
            (it.t1 == 0L && (it.t2.successful || it.t2.existedLocally)) || it.t2.pulledChanges
         }
         .flatMap {
            try {
               filePackageLoader.start()
            } catch (e: Exception) {
               gitStatusSink.emitNext(
                  LoaderStatus.error("Failed to read git repository from disk: ${e.message ?: e::class.simpleName!!}"),
                  Sinks.EmitFailureHandler.FAIL_FAST
               )
               Flux.empty()
            }
         }
         .filter { p -> p != null }
         .doOnNext { p ->
            logger.debug { "New source package event: ${p.hashCode()}" }
         }
         .distinctUntilChanged()
   }

   fun syncNow(): GitSyncStatus {
      logger.info { "syncing git repo to $workingDir" }
      val status = updateLoaderStatus(GitRepoSync.syncNow(workingDir, config))
      logger.info { "synced git repo to $workingDir" }
      return status
   }

   private fun updateLoaderStatus(syncStatus: GitSyncStatus):GitSyncStatus {
      val loaderStatus = when {
         syncStatus.successful && syncStatus.isClean -> LoaderStatus.OK
         !syncStatus.successful && syncStatus.existedLocally -> LoaderStatus.warning(
            syncStatus.errorMessage ?: "An unknown error occurred whilst pulling the git repository"
         )
         !syncStatus.successful -> LoaderStatus.error(
            syncStatus.errorMessage ?: "An unknown error occurred whilst pulling the git repository"
         )
         syncStatus.successful && !syncStatus.isClean -> LoaderStatus.warning(
            syncStatus.description
         )
         // You shouldn't hit this
         else -> error("The loader is in an unknown state: ${syncStatus}")
      }
      gitStatusSink.emitNext(loaderStatus, RetryFailOnSerializeEmitHandler)
      return syncStatus
   }

   override val root: URI
      get() = filePackageLoader.root

   override fun listUris(): Flux<URI> {
      val gitRoot = workingDir.resolve(".git/")
      return filePackageLoader.listUris()
         .filter { uri ->
            val path = uri.toPath()
            when {
               // Ignore .git content
               path.startsWith(gitRoot) -> false
               // Don't provide the root directory
               path == workingDir -> false
               else -> true
            }
         }
   }

   override fun readUri(uri: URI): Mono<ByteArray> {
      return filePackageLoader.readUri(uri)
   }

   override fun isEditable(): Boolean {
      return true
   }

   override fun createChangeset(name: String): Mono<CreateChangesetResponse> {
      return mono {
         val createdBranchName = GitReadWriteOperations(workingDir.toFile(), config).createBranch(name)
         currentBranch = createdBranchName
      }
         .map {
            CreateChangesetResponse(
               Changeset(
                  name,
                  isActive = true,
                  isDefault = false,
                  packageIdentifier = packageIdentifier
               )
            )
         }
   }

   override fun addChangesToChangeset(
      name: String,
      edits: List<VersionedSource>
   ): Mono<AddChangesToChangesetResponse> {
      val writer = FileSystemPackageWriter()
      return writer.writeSources(filePackageLoader, edits).map {
         GitReadWriteOperations(workingDir.toFile(), config).commitAndPush(name)
         val changesetOverview = GitReadWriteOperations(workingDir.toFile(), config).getChangesetOverview(name)
         AddChangesToChangesetResponse(changesetOverview)
      }
   }

   override fun finalizeChangeset(name: String): Mono<FinalizeChangesetResponse> {
      // TODO Access the user information from the authentication
      // TODO Allow specifying a description
      return mono { GitReadWriteOperations(workingDir.toFile(), config).raisePr(name, "", "Martin Pitt") }
         .map { (changesetOverview, link) ->
            FinalizeChangesetResponse(
               changesetOverview,
               Changeset(
                  name,
                  isActive = true,
                  isDefault = false,
                  packageIdentifier = packageIdentifier
               ),
               link
            )
         }
   }

   override fun updateChangeset(name: String, newName: String): Mono<UpdateChangesetResponse> {
      return mono {
         GitReadWriteOperations(workingDir.toFile(), config).renameCurrentBranch(newName)
         currentBranch = newName
      }
         .map {
            UpdateChangesetResponse(
               Changeset(
                  newName,
                  isActive = true,
                  isDefault = false,
                  packageIdentifier = packageIdentifier
               )
            )
         }
   }

   override fun getAvailableChangesets(): Mono<AvailableChangesetsResponse> {
      return mono { GitReadWriteOperations(workingDir.toFile(), config).getBranches() }
         .map { branchNames ->
            AvailableChangesetsResponse(branchNames
               .map { branchName ->
                  val prefix = config.pullRequestConfig?.branchPrefix ?: ""
                  val changesetBranch =
                     if (currentBranch == defaultBranchName) currentBranch else currentBranch.substringAfter(
                        prefix
                     )
                  Changeset(
                     branchName,
                     changesetBranch == branchName,
                     changesetBranch == defaultBranchName,
                     packageIdentifier = packageIdentifier
                  )
               })
         }
   }

   override fun setActiveChangeset(branchName: String): Mono<SetActiveChangesetResponse> {
      return mono {
         val resolvedBranchName = getResolvedBranchName(branchName)
         currentBranch = resolvedBranchName
         syncNow()
         val changesetOverview =
            GitReadWriteOperations(workingDir.toFile(), config).getChangesetOverview(resolvedBranchName)
         SetActiveChangesetResponse(
            Changeset(branchName, true, branchName == defaultBranchName, packageIdentifier),
            changesetOverview
         )
      }
   }

   private fun getResolvedBranchName(branchName: String): String {
      return if (branchName == config.branch) config.branch else config.pullRequestConfig?.branchPrefix + branchName
   }

   override val packageIdentifier: PackageIdentifier
      get() = filePackageLoader.packageIdentifier

   override fun loadTaxiProject(): Mono<Pair<Path, TaxiPackageProject>> {
      return filePackageLoader.loadTaxiProject()
   }

   override fun configureWriter(writer: ConfigSourceWriter): ConfigSourceWriter {
      return GitWriterDecorator(writer)
   }
}

/**
 * Simple decorator which appends UI messaging to write operations, indicating that
 * the user still needs to perform a commit and push
 */
class GitWriterDecorator(private val writer: ConfigSourceWriter) : ConfigSourceWriter by writer {
   companion object {
      val GIT_COMMIT_NEEDED = Message(
         Severity.WARNING,
         "Files have been changed locally, but have not been committed or pushed to the git repository - you'll need to do this manually. \n\nPulls from the git repository may fail while there are uncommitted changes."
      )
   }

   override fun save(source: VersionedSource): ResultWithMessage {
      return appendGitWarning(writer.save(source))
   }

   override fun saveConfig(updated: Config): ResultWithMessage {
      return appendGitWarning(writer.saveConfig(updated))
   }

   private fun appendGitWarning(result: ResultWithMessage): ResultWithMessage {
      return result.append(GIT_COMMIT_NEEDED, replaceIfExists = ResultWithMessage.SUCCESS_MESSAGE)
   }

}
