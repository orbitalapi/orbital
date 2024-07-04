package com.orbitalhq.schemaServer.core.repositories

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Throwables
import com.google.common.collect.Sets
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.ChangeWatchingConfigFileRepository
import com.orbitalhq.config.toHocon
import com.orbitalhq.schema.publisher.ProjectLoaderManager
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schema.publisher.loaders.ProjectTransportConfig
import com.orbitalhq.schemaServer.core.adaptors.InstantHoconSupport
import com.orbitalhq.schemaServer.core.adaptors.PackageLoaderSpecHoconSupport
import com.orbitalhq.schemaServer.core.adaptors.UriHoconSupport
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.git.GitSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecRemovedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.GitSpecRemovedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.toPackageMetadata
import com.orbitalhq.toVynePackageIdentifier
import com.orbitalhq.utils.concat
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.github.config4k.registerCustomType
import lang.taxi.packages.ProjectName
import lang.taxi.packages.TaxiPackageLoader
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.writers.ConfigWriter
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmissionException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText

class FileWorkspaceConfigLoader(
   val configFilePath: Path,
   fallback: Config = ConfigFactory.systemEnvironment(),
   private val eventDispatcher: ProjectSpecLifecycleEventDispatcher,
   private val projectManager: ProjectLoaderManager,
   emitStateOnInit: Boolean = true
) :
   ChangeWatchingConfigFileRepository<WorkspaceConfig>(
      configFilePath, fallback
   ), WorkspaceConfigLoader {
   private val logger = KotlinLogging.logger {}

   private var lastConfig: WorkspaceConfig? = null
   private val stateSink = Sinks.many().replay().latest<LoaderStatus>()
   override val loaderStatus: Flux<LoaderStatus> = stateSink.asFlux()
//      .distinctUntilChanged()
      .doOnNext { status -> logger.info { "File workspace loader at $configFilePath changed state: $status" } }


   init {
      registerCustomType(PackageLoaderSpecHoconSupport)
      registerCustomType(UriHoconSupport)
      registerCustomType(InstantHoconSupport)
      stateSink.emitNext(LoaderStatus.STARTING, Sinks.EmitFailureHandler.FAIL_FAST)

      if (emitStateOnInit) {
         readCurrentStateAndEmit()
      }
      watchForChanges()
   }

   override val isReadOnly: Boolean = false

   override fun filePathChanged(changedPath: Path) {
      super.filePathChanged(changedPath)
      readCurrentStateAndEmit()

   }

   /**
    * Reads the current state of the workspace config from disk, and emits
    * the relevant updates
    */
   fun readCurrentStateAndEmit(): WorkspaceConfig? {
      val oldConfig = lastConfig
      try {
         val workspaceConfig = load()
         logger.info { "Repository config at $configFilePath loaded with ${workspaceConfig.repoCountDescription()}" }
         emitUpdateEvents(oldConfig, workspaceConfig)
         lastConfig = workspaceConfig
      } catch (e: Exception) {
         // load() handles the errors in case there is an error in workspace.config and reports these errors back to UI.
         // However, it re-throws the caught error, we re-catch it over here so that Orbital doesn't fall over during
         // start-up
         val rootCause = Throwables.getRootCause(e)
         if (rootCause is EmissionException) {
            // This isn't a config issue, it's an application issue
            logger.error(rootCause) { "Failed to send update to changes to workspace" }
            throw rootCause
         }
         logger.error(rootCause) { "error in emitting workspace specs" }
         lastConfig = null
      }

      return lastConfig
   }

   private fun emitUpdateEvents(oldConfig: WorkspaceConfig?, workspaceConfig: WorkspaceConfig) {
      val oldFileSpecs = oldConfig?.fileConfigOrDefault?.projects?.toSet() ?: emptySet()
      val newFileSpecs = workspaceConfig.fileConfigOrDefault.projects.toSet()
      val removedPackages = mutableListOf<PackageIdentifier>()
      val fileSpecsRemoved = Sets.difference(oldFileSpecs, newFileSpecs)
      fileSpecsRemoved.forEach { removedFileSpec ->
         removedPackages.addAll(getPackageIdentifierForTransport(removedFileSpec))
         eventDispatcher.fileRepositorySpecRemoved(FileSpecRemovedEvent(removedFileSpec))
      }
      val fileSpecsAdded = Sets.difference(newFileSpecs, oldFileSpecs)
      fileSpecsAdded.forEach { addedFileSpec ->
         eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(addedFileSpec, workspaceConfig.fileConfigOrDefault))
      }

      val oldGitSpecs = oldConfig?.gitConfigOrDefault?.repositories?.toSet() ?: emptySet()
      val newGitSpecs = workspaceConfig.gitConfigOrDefault.repositories.toSet()
      val gitSpecsRemoved = Sets.difference(oldGitSpecs, newGitSpecs)
      gitSpecsRemoved.forEach { removedGitSpec ->
         removedPackages.addAll(getPackageIdentifierForTransport(removedGitSpec))
         eventDispatcher.gitRepositorySpecRemoved(GitSpecRemovedEvent(removedGitSpec))
      }

      val addedGitSpecs = Sets.difference(newGitSpecs, oldGitSpecs)
      addedGitSpecs.forEach { addedGitSpec ->
         eventDispatcher.gitRepositorySpecAdded(GitSpecAddedEvent(addedGitSpec, workspaceConfig.gitConfigOrDefault))
      }

      eventDispatcher.schemaSourceRemoved(removedPackages)

   }

   /**
    * Looks up the package identifier(s) for the transport.
    * Although some transports expose the package identifier directly (like file transports),
    * others - like git - need to defer to their loader, since the identifier isn't known until
    * we fetch the actual package.
    * So, we consistently leverage the projectManager to find the package identifier
    */
   private fun getPackageIdentifierForTransport(transport: ProjectTransportConfig): List<PackageIdentifier> {
      val packages = this.projectManager.loaders.filter {
         it.config == transport
      }.map { it.packageIdentifier }
      if (packages.isEmpty()) {
         logger.warn { "Could not find a corresponding package for transport $transport - sources from the package may not be updated" }
      }
      return packages
   }

   override fun extract(config: Config): WorkspaceConfig = config.extract()

   override fun emptyConfig(): WorkspaceConfig = WorkspaceConfig(null, null)

   override fun safeConfigJson(): String {
      return getSafeConfigString(unresolvedConfig(), asJson = true)
   }

   override fun load(createDefaultIfAbsent: Boolean): WorkspaceConfig {
      try {
         if (!createDefaultIfAbsent && !configFilePath.exists()) {
            throw IllegalStateException("No workspace file exists at $configFilePath")
         }
         val original = typedConfig()
         val config = resolveRelativePaths(original)
         // Use busy loop here, otherwise we get errors about non-serialized event emmission
         stateSink.emitNext(LoaderStatus.OK, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(2L)))
         this.lastConfig = config
         return config
      } catch (e: Exception) {
         val rootCause = Throwables.getRootCause(e)
         val message =
            "Error when loading workspace config file at $configFilePath: ${rootCause.message ?: rootCause::class.simpleName}"
         logger.warn { message }
         stateSink.emitNext(LoaderStatus.error(message), Sinks.EmitFailureHandler.FAIL_FAST)
         throw e
      }

   }

   private fun makeRelativeToConfigFile(path: Path): Path {
      return if (path.isAbsolute) {
         path
      } else {
         configFilePath.parent.resolve(path)
      }
   }

   private fun resolveRelativePaths(original: WorkspaceConfig): WorkspaceConfig {
      val updatedFileConfig = original.file?.let { fileConfig ->
         val resolvedPaths = fileConfig.projects
            .map { packageSpec ->
               val relativePath = makeRelativeToConfigFile(packageSpec.path)
               if (packageSpec.loader is TaxiPackageLoaderSpec) {
                  val packageMetadata = try {
                     // If we were passed a file, use it. Otherwise, if it's a dir, resolve taxi.conf file.
                     val pathToLoad = when {
                        !Files.exists(relativePath) -> error("No file or directory exists at ${relativePath.toFile().canonicalPath}")
                        relativePath.isDirectory() -> relativePath.resolve("taxi.conf")
                        relativePath.isRegularFile() -> relativePath
                        else -> error("Provided path is neither a file not a directory - not sure what to do")
                     }
                     TaxiPackageLoader(pathToLoad).load()?.toPackageMetadata()
                  } catch (e: Exception) {
                     val rootCause = Throwables.getRootCause(e)
                     logger.warn(e) { "Failed to read package metadata for project at $relativePath  ${rootCause.message}" }
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

   override fun addFileSpec(fileSpec: FileSystemPackageSpec): ModifyWorkspaceResponse {
      val current = this.typedConfig() // Don't call load, as we want the original, not the one we resolve paths with
      val currentFileConfig = current.file ?: FileSystemSchemaRepositoryConfig()

      if (currentFileConfig.projects.any { it.path == fileSpec.path }) {
         return ModifyWorkspaceResponse(ModifyProjectResponseStatus.Failed, "${fileSpec.path} already exists")
      }

      if (fileSpec.loader is TaxiPackageLoaderSpec) {
         if (fileSpec.packageIdentifier != null) {
            createProjectIfNotExists(fileSpec)
         } else {
            verifyProjectExists(fileSpec)
         }
      }

      val updated = current.copy(
         file = currentFileConfig.copy(
            projects = currentFileConfig.projects.concat(fileSpec)
         )
      )
      save(updated)
      eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(fileSpec, updated.file!!))
      return ModifyWorkspaceResponse(ModifyProjectResponseStatus.Ok)
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
      val taxiConfPath = taxiPackageLoader.taxiConfFilePath!!
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

   override fun addGitSpec(gitSpec: GitProjectStoreSpec): ModifyWorkspaceResponse {
      val current = this.typedConfig() // Don't call load, as we want the original, not the one we resolve paths with
      val currentGitConfig = current.git ?: GitSchemaRepositoryConfig()
      if (currentGitConfig.repositories.any { it.name == gitSpec.name }) {
         return ModifyWorkspaceResponse(
            ModifyProjectResponseStatus.Failed,
            "A git repository with the name ${gitSpec.name} already exists"
         )
      }

      if (currentGitConfig.repositories.any { it.uri == gitSpec.uri }) {
         return ModifyWorkspaceResponse(
            ModifyProjectResponseStatus.Failed,
            "A git repository already exists for ${gitSpec.uri}"
         )
      }

      val updated = current.copy(
         git = currentGitConfig.copy(
            repositories = currentGitConfig.repositories.concat(gitSpec)
         )
      )
      save(updated)
      eventDispatcher.gitRepositorySpecAdded(GitSpecAddedEvent(gitSpec, updated.git!!))
      return ModifyWorkspaceResponse(
         ModifyProjectResponseStatus.Ok,
         "${gitSpec.name} added as a new project, you need to commit and push  your workspace.conf"
      )
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

   override fun removeFileRepository(
      repositoryPath: Path,
      packageIdentifier: PackageIdentifier
   ): List<PackageIdentifier> {
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

   override fun removePushedRepository(identifier: PackageIdentifier): List<PackageIdentifier> {
      val identifiers = listOf(identifier)
      eventDispatcher.schemaSourceRemoved(identifiers)
      return identifiers
   }

   @VisibleForTesting
   internal fun getSavableHocon(workspaceConfig: WorkspaceConfig): Config {
      val newConfig = workspaceConfig.toHocon()

      // Use the existing unresolvedConfig to ensure that when we're
      // writing back out, that tokens that have been resolved
      // aren't accidentally written with their real values back out
      val existingValues = unresolvedConfig()

      val updated = ConfigFactory.empty()
         .withFallback(newConfig)
         .withFallback(existingValues)

      return updated
   }

   @VisibleForTesting
   internal fun getHoconString(workspaceConfig: WorkspaceConfig): String {
      return getSafeConfigString(getSavableHocon(workspaceConfig))
   }

   fun save(workspaceConfig: WorkspaceConfig) {
      val saveable = getSavableHocon(workspaceConfig)
      saveConfig(saveable)
   }
}
