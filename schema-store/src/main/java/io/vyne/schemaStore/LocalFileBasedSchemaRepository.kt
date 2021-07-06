package io.vyne.schemaStore

import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import lang.taxi.packages.TaxiPackageLoader
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi

private val logger = KotlinLogging.logger {}

data class SourcesChangedMessage(val sources: List<VersionedSource>)

/**
 * A schema repository which will read and write from a local disk
 */
@ExperimentalPathApi
class LocalFileBasedSchemaRepository(
   private val localFilePath: Path,
   private val validatingStore: LocalValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient(),
   @Value("\${taxi.schema-increment-version-on-recompile:true}") private val incrementPreReleaseVersionOnChange: Boolean = true
) {
   private var lastVersion: Version? = null
   private val sourcesChangedSink = Sinks.many().multicast().onBackpressureBuffer<SourcesChangedMessage>()
   val sourcesChanged: Flux<SourcesChangedMessage> = sourcesChangedSink.asFlux()

   /**
    * Returns a set of VersionedSource present at the localFilePath
    */
   fun rebuildSourceList(): List<VersionedSource> {
      logger.info("Reloading file sources at $localFilePath")
      val (taxiConf, sourceRoot) = getProjectAndRoot()
      if (lastVersion == null) {
         lastVersion = resolveVersion(taxiConf)
         logger.info("Using version $lastVersion as base version")
      } else {
         if (incrementPreReleaseVersionOnChange) {
            lastVersion = lastVersion!!.incrementPreReleaseVersion()
         }
      }

      val sources = sourceRoot.toFile().walkBottomUp()
         .filter { it.extension == "taxi" }
         .map {
            val pathRelativeToSourceRoot = sourceRoot.relativize(it.toPath()).toString()
            VersionedSource(pathRelativeToSourceRoot, lastVersion.toString(), it.readText())
         }
         .toList()

      if (sources.isNotEmpty()) {
         logger.info("Recompiling ${sources.size} files")
         sourcesChangedSink.emitNext(SourcesChangedMessage(sources)) { _, emitResult ->
            logger.warn { "Failed to publish SourcesChangedMessage - $emitResult" }
            false
         }
      } else {
         logger.info("No sources were found at $localFilePath. I'll just wait here.")
      }
      return sources

   }

   private fun getProjectAndRoot(): Pair<TaxiPackageProject?, Path> {
      val taxiConf = getProjectConfigFile(localFilePath)
      val sourceRoot = getSourceRoot(localFilePath, taxiConf)
      return Pair(taxiConf, sourceRoot)
   }

   private fun getProjectConfigFile(projectHomePath: Path): TaxiPackageProject? {
      val projectFile = projectHomePath.resolve("taxi.conf")
      return if (Files.exists(projectFile)) {
         logger.info("Found taxi.conf file at $projectFile - will use this for config")
         try {
            TaxiPackageLoader().withConfigFileAt(projectFile).load()
         } catch (e: Exception) {
            logger.error("Failed to read config file", e)
            null
         }
      } else {
         null
      }
   }

   private fun getSourceRoot(projectHomePath: Path, taxiPackageProject: TaxiPackageProject?): Path {
      return if (taxiPackageProject == null) {
         projectHomePath
      } else {
         projectHomePath.resolve(taxiPackageProject.sourceRoot)
      }
   }

   private fun resolveVersion(taxiPackageProject: TaxiPackageProject?): Version {
      val defaultVersion = Version.valueOf("0.1.0")
      return if (taxiPackageProject == null) {
         defaultVersion
      } else {
         try {
            Version.valueOf(taxiPackageProject.version)
         } catch (e: Exception) {
            logger.error(
               "Failed to parse version of ${taxiPackageProject.version}, will use defaultVersion of $defaultVersion",
               e
            )
            defaultVersion
         }
      }

   }

   fun writeSource(changed: VersionedSource) {
      val (taxiConf, sourceRoot) = getProjectAndRoot()
      if (taxiConf == null) {
         // No real good reason for this, but I feel like we should be operating inside a project.
         // Can relax this if needed
         error("A taxi project is required in order to make changes")
      }
      writeSource(sourceRoot, changed)
   }

   private fun writeSource(sourceRoot: Path, modifiedSource: VersionedSource) {
      val sourcePath = sourceRoot.resolve(modifiedSource.name)
      sourcePath.parent.toFile().mkdirs()
      sourcePath.toFile().writeText(modifiedSource.content)
      logger.info { "Source file $sourcePath updated" }
   }

}
