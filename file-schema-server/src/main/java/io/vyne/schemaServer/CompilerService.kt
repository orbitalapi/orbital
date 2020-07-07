package io.vyne.schemaServer

import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.utils.log
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiPackageLoader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct

@Component
class CompilerService(@Value("\${taxi.schema-local-storage}") val projectHome: String,
                      val schemaStoreClient: SchemaStoreClient) {

   private var counter = 0
   private var lastVersion: Version? = null

   fun recompile(incrementVersion: Boolean = true) {
      counter++
      log().info("Starting to recompile sources at $projectHome")
      val projectHomePath: Path = Paths.get(projectHome!!)
      val taxiConf = getProjectConfigFile(projectHomePath)
      val sourceRoot = getSourceRoot(projectHomePath, taxiConf)
      if (lastVersion == null) {
         lastVersion = resolveVersion(taxiConf)
         log().info("Using version $lastVersion as base version")
      } else {
         if(incrementVersion) {
            lastVersion = lastVersion!!.incrementPatchVersion()
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
         log().info("Recompiling ${sources.size} files")
         schemaStoreClient.submitSchemas(sources)
      } else {
         log().warn("No sources were found at $projectHome. I'll just wait here.")
      }

   }

   private fun getProjectConfigFile(projectHomePath: Path): TaxiPackageProject? {
      val projectFile = projectHomePath.resolve("taxi.conf")
      return if (Files.exists(projectFile)) {
         log().info("Found taxi.conf file at $projectFile - will use this for config")
         try {
            TaxiPackageLoader().withConfigFileAt(projectFile).load()
         } catch (e: Exception) {
            log().error("Failed to read config file", e)
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
            log().error("Failed to parse version of ${taxiPackageProject.version}, will use defaultVersion of $defaultVersion", e)
            defaultVersion
         }
      }

   }

}
