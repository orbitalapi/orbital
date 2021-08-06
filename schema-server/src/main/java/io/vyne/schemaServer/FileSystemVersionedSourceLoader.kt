package io.vyne.schemaServer

import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import lang.taxi.packages.TaxiPackageLoader
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

@ConditionalOnProperty(
   name = ["taxi.schema-local-storage"]
)
@Component
final class FileSystemVersionedSourceLoader(
    @Value("\${taxi.schema-local-storage}") val projectHome: String,
) : VersionedSourceLoader {

   private val logger = KotlinLogging.logger {}

   private val projectHomePath: Path = Paths.get(projectHome)
   private val lastVersion: AtomicReference<Version?> = AtomicReference(null)

   override fun loadVersionedSources(incrementVersion: Boolean): List<VersionedSource> {
      logger.info("Loading sources at $projectHome")

      val taxiConf = getProjectConfigFile(projectHomePath)
      val sourceRoot = getSourceRoot(projectHomePath, taxiConf)

      val newVersion = lastVersion.updateAndGet { currentVal ->
         when {
            currentVal == null -> {
               val version = resolveVersion(taxiConf)
               logger.info("Using version $version as base version")
               version
            }
            incrementVersion -> currentVal.incrementPatchVersion()
            else -> currentVal
         }
      }!!

      val sources = sourceRoot.toFile().walkBottomUp()
         .filter { it.extension == "taxi" }
         .map { file ->
            val pathRelativeToSourceRoot =
               sourceRoot.relativize(file.toPath()).toString()
             VersionedSource(
                 name = pathRelativeToSourceRoot,
                 version = newVersion.toString(),
                 content = file.readText()
             )
         }
         .toList()
      return sources
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

   private fun getSourceRoot(
       projectHomePath: Path,
       taxiPackageProject: TaxiPackageProject?
   ): Path {
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
}
