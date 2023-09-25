package com.orbitalhq.schemaServer.core.repositories

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.github.config4k.registerCustomType
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.BaseHoconConfigFileRepository
import com.orbitalhq.config.toHocon
import com.orbitalhq.schemaServer.core.adaptors.InstantHoconSupport
import com.orbitalhq.schemaServer.core.adaptors.PackageLoaderSpecHoconSupport
import com.orbitalhq.schemaServer.core.adaptors.UriHoconSupport
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitRepositorySpec
import com.orbitalhq.schemaServer.core.git.GitSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.toPackageMetadata
import com.orbitalhq.toVynePackageIdentifier
import com.orbitalhq.utils.concat
import lang.taxi.packages.ProjectName
import lang.taxi.packages.TaxiPackageLoader
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.writers.ConfigWriter
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.*

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
         val resolvedPaths = fileConfig.projects
            .map { packageSpec ->
               val relativePath = makeRelativeToConfigFile(packageSpec.path)
               if (packageSpec.loader is TaxiPackageLoaderSpec) {
                  val packageMetadata = try {
                     // If we were passed a file, use it. Otherwise, if it's a dir, resolve taxi.conf file.
                     val pathToLoad = when {
                        relativePath.isDirectory() -> relativePath.resolve("taxi.conf")
                        relativePath.isRegularFile() -> relativePath
                        else -> error("Provided path is neither a file not a directory - not sure what to do")
                     }
                     TaxiPackageLoader(pathToLoad).load()?.toPackageMetadata()
                  } catch (e: Exception) {
                     logger.warn(e) { "Failed to read package metadata for project at $relativePath  ${e.message ?: e.cause?.message}" }
                     null
                  }
                  packageSpec.copy(path = relativePath, packageIdentifier = packageMetadata?.identifier)
               } else {
                  packageSpec.copy(path = relativePath)
               }


            }
         fileConfig.copy(projects = resolvedPaths)
      }
      return original.copy(file = updatedFileConfig)
   }

   override fun addFileSpec(fileSpec: FileSystemPackageSpec) {
      val current = this.typedConfig() // Don't call load, as we want the original, not the one we resolve paths with
      val currentFileConfig = current.file ?: FileSystemSchemaRepositoryConfig()

      require(currentFileConfig.projects.none { it.path == fileSpec.path }) { "${fileSpec.path} already exists" }
      val packageIdentifier = if (fileSpec.packageIdentifier != null) {
         createProjectIfNotExists(fileSpec)
      } else {
         verifyProjectExists(fileSpec)
      }

      val fileSpecWithPackageIdentifier = fileSpec.copy(packageIdentifier = packageIdentifier)


      val updated = current.copy(
         file = currentFileConfig.copy(
            projects = currentFileConfig.projects.concat(
               fileSpecWithPackageIdentifier
            )
         )
      )
      save(updated)
      eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(fileSpec, updated.file!!))
   }

   private fun verifyProjectExists(fileSpec: FileSystemPackageSpec): PackageIdentifier {
      return when (fileSpec.loader) {
         is TaxiPackageLoaderSpec -> verifyTaxiProjectExists(fileSpec)
         is OpenApiPackageLoaderSpec -> verifyOpenApiProjectExists(fileSpec)
         else -> error("No package verification built for loader type ${fileSpec.loader::class.simpleName}")
      }

   }

   private fun verifyOpenApiProjectExists(fileSpec: FileSystemPackageSpec): PackageIdentifier {
      require(fileSpec.path.exists()) { "No OpenAPI spec found at ${fileSpec.path}" }
      // What else do we need to check?
      return (fileSpec.loader as OpenApiPackageLoaderSpec).identifier
   }

   private fun verifyTaxiProjectExists(fileSpec: FileSystemPackageSpec): PackageIdentifier {
      val project = TaxiPackageLoader.forDirectoryContainingTaxiFile(fileSpec.path).load()
      if (fileSpec.packageIdentifier != null && project.identifier.toVynePackageIdentifier() != fileSpec.packageIdentifier) {
         error("The provided package identifier (${fileSpec.packageIdentifier!!.id} does not match the package identifier found at ${fileSpec.path} - ${project.identifier.id}")
      }
      return project.identifier.toVynePackageIdentifier()
   }

   private fun createProjectIfNotExists(fileSpec: FileSystemPackageSpec): PackageIdentifier {
      // We don't create openAPI projects
      if (fileSpec.loader is OpenApiPackageLoaderSpec) {
         return verifyOpenApiProjectExists(fileSpec)
      }

      val path = fileSpec.path
      if (!path.createDirectories().exists()) {
         logger.warn { "Failed to create directory $path for taxi project" }
         error("Failed to create directory $path for taxi project")
      }
      val taxiPackageLoader = TaxiPackageLoader.forDirectoryContainingTaxiFile(fileSpec.path)
      val taxiConfPath = taxiPackageLoader.path!!
      if (!taxiConfPath.exists()) {
         if (fileSpec.packageIdentifier == null) {
            error("There is no Taxi project at ${fileSpec.path}, however cannot create an empty one as a package identifier wasn't provided")
         }
         logger.info { "No taxi.conf exists at $taxiConfPath - creating one" }
         val project = TaxiPackageProject(
            name = ProjectName(fileSpec.packageIdentifier.organisation, fileSpec.packageIdentifier.name).id,
            version = fileSpec.packageIdentifier.version,
            sourceRoot = "src/"
         )
         val taxiConf = ConfigWriter().writeMinimal(project)
         taxiConfPath.writeText(taxiConf)
         path.resolve(project.sourceRoot).createDirectories()
      }

      return taxiPackageLoader.load().identifier.toVynePackageIdentifier()


   }

   override fun addGitSpec(gitSpec: GitRepositorySpec) {
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

   override fun removeGitRepository(
      repositoryName: String,
      packageIdentifier: PackageIdentifier
   ): List<PackageIdentifier> {
      val original = load()
      val matchedProjects = original.git?.repositories?.filter { it.name == repositoryName }
      require(matchedProjects?.size == 1) { "Could not find git repository with name $repositoryName" }
      val updatedProjectList = original.git!!.repositories.toMutableList()
      updatedProjectList.removeIf { it.name == repositoryName }
      val updatedConfig = original.copy(
         git = original.git!!.copy(repositories = updatedProjectList),
      )
      save(updatedConfig)
      logger.info { "Removed git repository $repositoryName and saved to disk" }
      val affectedPackages = listOf(packageIdentifier)
      eventDispatcher.schemaSourceRemoved(affectedPackages)
      return affectedPackages
   }

   override fun removeFileRepository(packageIdentifier: PackageIdentifier): List<PackageIdentifier> {
      val original = this.load()
      val matchedProjects =
         original.file?.projects?.filter { it.packageIdentifier?.uriSafeId == packageIdentifier.uriSafeId }
      require(matchedProjects?.size == 1) { "Could not find file repository for package $packageIdentifier" }
      val updatedProjectList = original.file!!.projects.toMutableList()
      updatedProjectList.removeIf { it.packageIdentifier!!.uriSafeId == packageIdentifier.uriSafeId }
      val updatedConfig = original.copy(
         file = original.file!!.copy(projects = updatedProjectList),
      )
      save(updatedConfig)
      logger.info { "Removed file repository for $packageIdentifier and saved to disk" }
      matchedProjects!!.map { it.packageIdentifier }
      val removedPackages = listOf(packageIdentifier)
      eventDispatcher.schemaSourceRemoved(removedPackages)
      return removedPackages
   }

   fun save(schemaRepoConfig: SchemaRepositoryConfig) {
      val newConfig = schemaRepoConfig.toHocon()

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
