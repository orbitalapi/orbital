package io.vyne.schemaServer.core.git

import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.publisher.loaders.AddChangesToChangesetResponse
import io.vyne.schema.publisher.loaders.AvailableChangesetsResponse
import io.vyne.schema.publisher.loaders.Changeset
import io.vyne.schema.publisher.loaders.CreateChangesetResponse
import io.vyne.schema.publisher.loaders.FinalizeChangesetResponse
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schema.publisher.loaders.SchemaSourcesAdaptor
import io.vyne.schema.publisher.loaders.SetActiveChangesetResponse
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoader
import io.vyne.schemaServer.core.file.packages.FileSystemPackageWriter
import io.vyne.schemaServer.core.file.packages.ReactiveFileSystemMonitor
import io.vyne.schemaServer.core.file.packages.ReactiveWatchingFileSystemMonitor
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.toPath

class GitSchemaPackageLoader(
   val workingDir: Path,
   private val config: GitRepositoryConfig,
   adaptor: SchemaSourcesAdaptor,
   // visible for testing
   val fileMonitor: ReactiveFileSystemMonitor = ReactiveWatchingFileSystemMonitor(workingDir, listOf(".git")),
   val gitPollFrequency: Duration = Duration.ofSeconds(30),
) : SchemaPackageTransport {

   object PollEvent

   private val logger = KotlinLogging.logger {}

   private val ticker = Flux.interval(gitPollFrequency).map { PollEvent }
   private val filePackageLoader: FileSystemPackageLoader

   private var currentBranch = config.branch

   init {
      val safePath = if (config.path.startsWith(FileSystems.getDefault().separator)) {
         Paths.get(".${FileSystems.getDefault().separator}" + config.path)
      } else {
         config.path
      }
      val pathWithGitRepo = workingDir.resolve(safePath).normalize()
      filePackageLoader = FileSystemPackageLoader(
         config = FileSystemPackageSpec(
            pathWithGitRepo, config.loader
         ),
         adaptor = adaptor,
         fileMonitor = fileMonitor,
         transportDecorator = this
      )
   }

   override fun start(): Flux<SourcePackage> {
      syncNow()
      ticker.subscribe { syncNow() }
      return filePackageLoader.start()
   }

   fun syncNow() {
      logger.info { "Starting a git sync for ${getDescriptionText()}" }
      try {
         GitOperations(workingDir.toFile(), config.copy(branch = currentBranch)).fetchLatest()
      } catch (e: Exception) {
         logger.warn(e) { "Failed to complete git sync for ${getDescriptionText()}" }
         if (currentBranch != config.branch) {
            logger.info { "Reverting to default branch ${config.branch} due to a failed pull of $currentBranch" }
            currentBranch = config.branch
         }
      }

      logger.info { "Git sync for ${getDescriptionText()} finished" }
   }

   // TODO Remove this function and replace its usage with ${config.description}
   private fun getDescriptionText(): String {
      return "$config.name - $config.uri / $currentBranch"
   }

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
         currentBranch = name
         GitOperations(workingDir.toFile(), config).createBranch(name)
      }
         .map { CreateChangesetResponse(it) }
   }

   override fun addChangesToChangeset(name: String, edits: List<VersionedSource>): Mono<AddChangesToChangesetResponse> {
      val writer = FileSystemPackageWriter()
      return writer.writeSources(filePackageLoader, edits).map {
         GitOperations(workingDir.toFile(), config).commitAndPush(name)
         AddChangesToChangesetResponse()
      }
   }

   override fun finalizeChangeset(name: String): Mono<FinalizeChangesetResponse> {
      return mono { GitOperations(workingDir.toFile(), config).raisePr(name, "", "Martin Pitt") }
         .map { FinalizeChangesetResponse(it) }
   }

   override fun getAvailableChangesets(): Mono<AvailableChangesetsResponse> {
      return mono { GitOperations(workingDir.toFile(), config).getBranches() }
         .map { branchNames -> AvailableChangesetsResponse(branchNames
            .map {
               val prefix = config.updateFlowConfig?.branchPrefix ?: ""
               val branchName = if (currentBranch == "main") currentBranch else currentBranch.substringAfter(prefix)
               Changeset(it, branchName == it) })
         }
   }

   override fun setActiveChangeset(branchName: String): Mono<SetActiveChangesetResponse> {
      return mono {
         val resolvedBranchName = if (branchName == config.branch) config.branch else config.updateFlowConfig?.branchPrefix + branchName
         currentBranch = resolvedBranchName
         syncNow()
      }
         .map { SetActiveChangesetResponse(true) }
   }

   override val packageIdentifier: PackageIdentifier
      get() = filePackageLoader.packageIdentifier
}
