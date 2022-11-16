package io.vyne.schemaServer.core.repositories

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.github.config4k.registerCustomType
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.config.toConfig
import io.vyne.schemaServer.core.adaptors.InstantHoconSupport
import io.vyne.schemaServer.core.adaptors.PackageLoaderSpecHoconSupport
import io.vyne.schemaServer.core.adaptors.UriHoconSupport
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import io.vyne.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import io.vyne.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import io.vyne.utils.concat
import lang.taxi.packages.ProjectName
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class FileSchemaRepositoryConfigLoader(
   private val configFilePath: Path,
   fallback: Config = ConfigFactory.systemEnvironment(),
   private val eventDispatcher: RepositorySpecLifecycleEventDispatcher
) :
   BaseHoconConfigFileRepository<SchemaRepositoryConfig>(
      configFilePath, fallback
   ), SchemaRepositoryConfigLoader {
   private val logger = KotlinLogging.logger {}

   init {
      registerCustomType(PackageLoaderSpecHoconSupport)
      registerCustomType(UriHoconSupport)
      registerCustomType(InstantHoconSupport)

      emitInitialState()
   }

   private fun emitInitialState() {
      val initialConfig = load()
      logger.info { "Repository config at $configFilePath loaded with ${initialConfig.repoCountDescription()}" }
      initialConfig.file?.let { fileConfig ->
         fileConfig.projects
            .forEach { eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(it, fileConfig)) }
      }
      initialConfig.git?.let { gitConfig ->
         gitConfig.repositories.forEach { eventDispatcher.gitRepositorySpecAdded(GitSpecAddedEvent(it, gitConfig)) }
      }
   }

   override fun extract(config: Config): SchemaRepositoryConfig = config.extract()

   override fun emptyConfig(): SchemaRepositoryConfig = SchemaRepositoryConfig(null, null)

   override fun safeConfigJson(): String {
      return getSafeConfigString(unresolvedConfig(), asJson = true)
   }

   override fun load(): SchemaRepositoryConfig {
      val original = typedConfig()
      return resolveRelativePaths(original)
   }

   private fun makeRelativeToConfigFile(path: Path): Path {
      return if (path.isAbsolute) {
         path
      } else {
         configFilePath.parent.resolve(path)
      }
   }

   private fun resolveRelativePaths(original: SchemaRepositoryConfig): SchemaRepositoryConfig {
      val updatedFileConfig = original.file?.let { fileConfig ->
         val resolvedPaths = fileConfig.projects.map { projectPath ->
            projectPath.copy(path = makeRelativeToConfigFile(projectPath.path))
         }
         fileConfig.copy(projects = resolvedPaths)
      }
      return original.copy(file = updatedFileConfig)
   }

   override fun addFileSpec(fileSpec: FileSystemPackageSpec) {
      val current = this.typedConfig() // Don't call load, as we want the original, not the one we resolve paths with
      val currentFileConfig = current.file ?: FileSystemSchemaRepositoryConfig()

      require(currentFileConfig.projects.none { it.path == fileSpec.path }) { "${fileSpec.path} already exists" }
      if (fileSpec.packageIdentifier != null) {
         createProjectIfNotExists(fileSpec)
      } else {
         verifyProjectExists(fileSpec)
      }

      val updated = current.copy(
         file = currentFileConfig.copy(
            projects = currentFileConfig.projects.concat(
               fileSpec
            )
         )
      )
      save(updated)
      eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(fileSpec, updated.file!!))
   }

   private fun verifyProjectExists(fileSpec: FileSystemPackageSpec) {
      TODO("Not yet implemented")
   }

   private fun createProjectIfNotExists(fileSpec: FileSystemPackageSpec) {
      val packageIdentifier = fileSpec.packageIdentifier!!
      val path = fileSpec.path
      val taxiConfPath = path.resolve("taxi.conf")
      if (!path.createDirectories().exists()) {
         logger.warn { "Failed to create directory $path for taxi project" }
         error("Failed to create directory $path for taxi project")
      }
      if (!taxiConfPath.exists()) {
         logger.info { "No taxi.conf exists at $taxiConfPath - creating one" }
         val project = TaxiPackageProject(
            name = ProjectName(packageIdentifier.organisation, packageIdentifier.name).id,
            version = packageIdentifier.version,
            sourceRoot = "src/"
         )
//         val taxiConf = ConfigWriter().writeMinimal(project)
//         taxiConfPath.writeText(taxiConf)
         path.resolve(project.sourceRoot).createDirectories()
      }


   }

   override fun addGitSpec(gitSpec: GitRepositoryConfig) {
      val current = this.typedConfig() // Don't call load, as we want the original, not the one we resolve paths with
      val currentGitConfig = current.git ?: GitSchemaRepositoryConfig()

      require(currentGitConfig.repositories.none { it.name == gitSpec.name }) { "A git repository with the name ${gitSpec.name} already exists" }
      require(currentGitConfig.repositories.none { it.uri == gitSpec.uri }) { "A git repository already exists for ${gitSpec.uri}" }

      val updated = current.copy(
         git = currentGitConfig.copy(
            repositories = currentGitConfig.repositories.concat(gitSpec)
         )
      )
      save(updated)
      eventDispatcher.gitRepositorySpecAdded(GitSpecAddedEvent(gitSpec, updated.git!!))
   }

   fun save(schemaRepoConfig: SchemaRepositoryConfig) {
      val newConfig = schemaRepoConfig.toConfig()

      // Use the existing unresolvedConfig to ensure that when we're
      // writing back out, that tokens that have been resolved
      // aren't accidentally written with their real values back out
      val existingValues = unresolvedConfig()

      val updated = ConfigFactory.empty()
         .withFallback(newConfig)
         .withFallback(existingValues)

      saveConfig(updated)
   }
}
