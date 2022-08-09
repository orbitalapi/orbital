package io.vyne.schemaServer.core.git

import io.vyne.schema.api.SchemaPackage
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

class GitRepositorySchemaPackageLoader(
   val workingDir: Path,
   private val config: GitRepositoryConfig,
   adaptor: SchemaSourcesAdaptor,
   fileMonitor: ReactiveFileSystemMonitor = ReactiveWatchingFileSystemMonitor(workingDir),
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
         FileSystemPackageSpec(
            pathWithGitRepo, config.loader
         ), adaptor, fileMonitor
      )
   }

   override fun start(): Flux<SchemaPackage> {
      syncNow()
      ticker.subscribe { syncNow() }
      return filePackageLoader.start()
   }

   fun syncNow() {
      logger.info { "Starting a git sync for ${config.description}" }
      try {
         GitRepositorySourceLoader(workingDir.toFile(), config).fetchLatest()
      } catch (e: Exception) {
         logger.warn(e) { "Failed to complete git sync for ${config.description}" }
      }

      logger.info { "Git sync for ${config.description} finished" }
   }

   override fun listUris(): Flux<URI> {
      return filePackageLoader.listUris()
   }

   override fun readUri(uri: URI): Mono<ByteArray> {
      return filePackageLoader.readUri(uri)
   }
}
