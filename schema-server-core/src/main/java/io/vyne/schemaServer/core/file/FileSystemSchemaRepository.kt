package io.vyne.schemaServer.core.file

import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.publisher.loaders.FileSchemaSourceProvider
import io.vyne.schema.publisher.loaders.FileSystemSchemaProjectLoader
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

data class SourcesChangedMessage(val packages: List<SourcePackage>)

/**
 * A schema repository which will read and write from a local disk.
 *
 * Wraps a FileSystemVersionSourceLoader to expose change events
 * and add file writing capabilities.
 *
 * The repository is not responsible for detecting file system changes.
 * That's left to a FileSystemMonitor (either FilePoller or FileWatcher),
 * which will notify this repository to reload its sources when changes are
 * detected
 *
 */
class FileSystemSchemaRepository(
   private val sourceLoader: FileSystemVersionedSourceLoader
) : io.vyne.schemaServer.core.VersionedSourceLoader by sourceLoader, io.vyne.schemaServer.core.UpdatingVersionedSourceLoader {

   companion object {
      fun forPath(path: String, incrementVersionOnChange: Boolean = false): FileSystemSchemaRepository {
         return forPath(Paths.get(path), incrementVersionOnChange)
      }

      fun forPath(path: Path, incrementVersionOnChange: Boolean = false): FileSystemSchemaRepository {
         return FileSystemSchemaRepository(
            FileSystemVersionedSourceLoader(FileSystemSchemaProjectLoader(path, incrementVersionOnChange))
         )
      }

   }

   val projectPath: Path = sourceLoader.projectPath

   private val sourcesChangedSink = Sinks.many().multicast().onBackpressureBuffer<SourcesChangedMessage>()
   override val sourcesChanged: Flux<SourcesChangedMessage> = sourcesChangedSink.asFlux()

   /**
    * Returns a set of VersionedSource present at the localFilePath.
    *
    * Causes a SourcesChangeMessage to be emitted containing the current set of sources.
    */
   fun refreshSources(): SourcePackage {
      logger.info("Reloading file sources at ${sourceLoader.identifier}")
      val sourcePackage = sourceLoader.loadSourcePackage()
      if (sourcePackage.sources.isNotEmpty()) {
         logger.info("Reload returned ${sourcePackage.sources.size} files - emitting SourcesChangedMessage")
         sourcesChangedSink.emitNext(SourcesChangedMessage(listOf(sourcePackage))) { signalType, emitResult ->
            logger.warn { "Failed to publish SourcesChangedMessage - $signalType $emitResult" }
            false
         }
      } else {
         logger.info("No sources were found at ${sourceLoader.identifier}. I'll just wait here.")
      }
      return sourcePackage
   }

   fun writeSource(changed: VersionedSource): VersionedSource {
      val (taxiConf, sourceRoot) = sourceLoader.projectAndRoot
      if (taxiConf == null) {
         // No real good reason for this, but I feel like we should be operating inside a project.
         // Can relax this if needed
         error("A taxi project is required in order to make changes")
      }
      return writeSource(taxiConf, sourceRoot, listOf(changed)).first()
   }

   fun writeSources(changed: List<VersionedSource>): List<VersionedSource> {
      val (taxiConf, sourceRoot) = sourceLoader.projectAndRoot
      if (taxiConf == null) {
         // No real good reason for this, but I feel like we should be operating inside a project.
         // Can relax this if needed
         error("A taxi project is required in order to make changes")
      }
      return writeSource(taxiConf, sourceRoot, changed)
   }

   private fun writeSource(
      project: TaxiPackageProject,
      sourceRoot: Path,
      modifiedSources: List<VersionedSource>
   ): List<VersionedSource> {
      return modifiedSources.map { modifiedSource ->
         val sourcePath = sourceRoot.resolve(modifiedSource.name)
         sourcePath.parent.toFile().mkdirs()
         sourcePath.toFile().writeText(modifiedSource.content)
         logger.info { "Source file $sourcePath updated" }
         // TODO : We should really be incrementing the version, or something here
         modifiedSource.copy(version = project.version)
      }
   }

}
