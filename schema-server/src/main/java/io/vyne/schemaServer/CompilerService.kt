package io.vyne.schemaServer

import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaPublisher
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiPackageLoader
import mu.KLogger
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class CompilerService(
   @Value("\${taxi.schema-local-storage}") val projectHome: String,
   val schemaPublisher: SchemaPublisher,
   private val logger: KLogger = KotlinLogging.logger {},
) {

   private var counter = 0
   private var lastVersion: Version? = null

   fun recompile(incrementVersion: Boolean = true) {
      counter++
      logger.info("Starting to recompile sources at $projectHome")
      val projectHomePath: Path = Paths.get(projectHome)
      val taxiConf = getProjectConfigFile(projectHomePath)
      val sourceRoot = getSourceRoot(projectHomePath, taxiConf)
      if (lastVersion == null) {
         lastVersion = resolveVersion(taxiConf)
         logger.info("Using version $lastVersion as base version")
      } else {
         if (incrementVersion) {
            lastVersion = lastVersion!!.incrementPatchVersion()
         }
      }

      val sources = sourceRoot.toFile().walkBottomUp()
         .filter { it.extension == "taxi" }
         .map { file ->
            val pathRelativeToSourceRoot =
               sourceRoot.relativize(file.toPath()).toString()
            VersionedSource(
               name = pathRelativeToSourceRoot,
               version = lastVersion.toString(),
               content = file.readText()
            )
         }
         .toList()

      if (sources.isNotEmpty()) {
         logger.info("Recompiling ${sources.size} files")
         schemaPublisher.submitSchemas(sources)
      } else {
         logger.warn("No sources were found at $projectHome. I'll just wait here.")
      }

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
