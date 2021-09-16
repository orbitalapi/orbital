package io.vyne.schemaServer.file

import io.vyne.VersionedSource
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

data class SourcesChangedMessage(val sources: List<VersionedSource>)

/**
 * A schema repository which will read and write from a local disk
 */
class FileSystemSchemaRepository(
   private val sourceLoader:FileSystemVersionedSourceLoader,
   private val incrementPreReleaseVersionOnChange: Boolean = false
) {

   companion object {
      fun forPath(path: Path, incrementPreReleaseVersionOnChange: Boolean = false): FileSystemSchemaRepository {
         return FileSystemSchemaRepository(
            FileSystemVersionedSourceLoader(path, incrementPreReleaseVersionOnChange)
         )
      }
   }

   private val sourcesChangedSink = Sinks.many().multicast().onBackpressureBuffer<SourcesChangedMessage>()
   val sourcesChanged: Flux<SourcesChangedMessage> = sourcesChangedSink.asFlux()

   /**
    * Returns a set of VersionedSource present at the localFilePath.
    *
    * Causes a SourcesChangeMessage to be emitted containing the current set of sources.
    */
   fun refreshSources(): List<VersionedSource> {
      logger.info("Reloading file sources at ${sourceLoader.identifier}")
      val sources = sourceLoader.loadVersionedSources()
      if (sources.isNotEmpty()) {
         logger.info("Reload returned ${sources.size} files - emitting SourcesChangedMessage")
         sourcesChangedSink.emitNext(SourcesChangedMessage(sources)) { signalType, emitResult ->
            logger.warn { "Failed to publish SourcesChangedMessage - $signalType $emitResult" }
            false
         }
      } else {
         logger.info("No sources were found at ${sourceLoader.identifier}. I'll just wait here.")
      }
      return sources

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
      val (taxiConf, sourceRoot) =  sourceLoader.projectAndRoot
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
