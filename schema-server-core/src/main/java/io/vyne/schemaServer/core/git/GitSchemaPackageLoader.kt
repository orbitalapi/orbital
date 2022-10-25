package io.vyne.schemaServer.core.git

import io.vyne.SourcePackage
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schema.publisher.loaders.SchemaSourcesAdaptor
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoader
import io.vyne.schemaServer.core.file.packages.ReactiveFileSystemMonitor
import io.vyne.schemaServer.core.file.packages.ReactiveWatchingFileSystemMonitor
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
   val fileMonitor: ReactiveFileSystemMonitor = ReactiveWatchingFileSystemMonitor(workingDir),
   val gitPollFrequency: Duration = Duration.ofSeconds(30),
) : SchemaPackageTransport {

   object PollEvent

   private val logger = KotlinLogging.logger {}

   private val ticker = Flux.interval(gitPollFrequency).map { PollEvent }
   private val filePackageLoader: FileSystemPackageLoader

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
         fileMontitor = fileMonitor,
         transportDecorator = this
      )
   }

   override fun start(): Flux<SourcePackage> {
      syncNow()
      ticker.subscribe { syncNow() }
      return filePackageLoader.start()
   }

   fun syncNow() {
      logger.info { "Starting a git sync for ${config.description}" }
      try {
         GitOperations(workingDir.toFile(), config).fetchLatest()
      } catch (e: Exception) {
         logger.warn(e) { "Failed to complete git sync for ${config.description}" }
      }

      logger.info { "Git sync for ${config.description} finished" }
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
}
