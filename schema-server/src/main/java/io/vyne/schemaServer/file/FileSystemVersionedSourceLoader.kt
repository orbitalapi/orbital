package io.vyne.schemaServer.file

import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import io.vyne.schemaServer.VersionedSourceLoader
import lang.taxi.packages.TaxiPackageLoader
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

final class FileSystemVersionedSourceLoader(
   val projectHomePath: Path,
   val incrementPreReleaseVersionOnChange: Boolean = true
) : VersionedSourceLoader {

   companion object {
      fun forProjectHome(projectHome: String): FileSystemVersionedSourceLoader {
         return FileSystemVersionedSourceLoader(Paths.get(projectHome))
      }
   }

   private val logger = KotlinLogging.logger {}

   private val lastVersion: AtomicReference<Version?> = AtomicReference(null)

   override val identifier: String = projectHomePath.toString()

   override fun loadVersionedSources(forceVersionIncrement: Boolean): List<VersionedSource> {
      logger.info("Loading sources at ${projectHomePath.toFile().canonicalPath}")

      val taxiConf = getProjectConfigFile(projectHomePath)
      val sourceRoot = getSourceRoot(projectHomePath, taxiConf)

      val newVersion = lastVersion.updateAndGet { currentVal ->
         when {
            currentVal == null -> {
               val version = resolveVersion(taxiConf)
               logger.info("Using version $version as base version")
               version
            }
            forceVersionIncrement -> currentVal.incrementPreReleaseVersion()
            incrementPreReleaseVersionOnChange -> currentVal.incrementPreReleaseVersion()
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

   val projectAndRoot: Pair<TaxiPackageProject?, Path>
      get() {
         val taxiConf = getProjectConfigFile()
         val sourceRoot = getSourceRoot(taxiConf)
         return Pair(taxiConf, sourceRoot)
      }

   private fun getProjectConfigFile(): TaxiPackageProject? {
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
