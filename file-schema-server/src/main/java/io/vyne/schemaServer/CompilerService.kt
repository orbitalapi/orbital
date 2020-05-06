package io.vyne.schemaServer

import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.utils.log
import lang.taxi.packages.ProjectConfig
import lang.taxi.packages.TaxiProjectLoader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct

@Component
class CompilerService(@Value("\${taxi.project-home}") val projectHome: String,
                      val schemaStoreClient: SchemaStoreClient) {

   private var counter = 0
   private var lastVersion: Version? = null

   @PostConstruct
   fun recompile() {
      counter++
      log().info("Starting to recompile sources at $projectHome")
      val projectHomePath: Path = Paths.get(projectHome)
      val taxiConf = getProjectConfigFile(projectHomePath)
      val sourceRoot = getSourceRoot(projectHomePath, taxiConf)
      if (lastVersion == null) {
         lastVersion = resolveVersion(taxiConf)
         log().info("Using version $lastVersion as base version")
      } else {
         lastVersion = lastVersion!!.incrementPatchVersion()
      }

      val schemas = sourceRoot.toFile().walkBottomUp()
         .filter { it.extension == "taxi" }
         .map {
            val pathRelativeToSourceRoot = sourceRoot.relativize(it.toPath()).toString()
            VersionedSource(pathRelativeToSourceRoot, lastVersion.toString(), it.readText())
         }
         .toList()
      log().info("Recompiling ${schemas.size} files")
      schemaStoreClient.submitSchemas(schemas)
   }

   private fun getProjectConfigFile(projectHomePath: Path): ProjectConfig? {
      val projectFile = projectHomePath.resolve("taxi.conf")
      return if (Files.exists(projectFile)) {
         log().info("Found taxi.conf file at $projectFile - will use this for config")
         try {
            TaxiProjectLoader().withConfigFileAt(projectFile).load()
         } catch (e: Exception) {
            log().error("Failed to read config file", e)
            null
         }
      } else {
         null
      }
   }

   private fun getSourceRoot(projectHomePath: Path, taxiConfig: ProjectConfig?): Path {
      return if (taxiConfig == null) {
         projectHomePath
      } else {
         projectHomePath.resolve(taxiConfig.sourceRoot)
      }
   }
   private fun resolveVersion(taxiConfig: ProjectConfig?): Version {
      val defaultVersion = Version.valueOf("0.1.0")
      return if (taxiConfig == null) {
         defaultVersion
      } else {
         try {
            Version.valueOf(taxiConfig.version)
         } catch (e: Exception) {
            log().error("Failed to parse version of ${taxiConfig.version}, will use defaultVersion of $defaultVersion", e)
            defaultVersion
         }
      }

   }

}
