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
import com.orbitalhq.schemaServer.core.config.WorkspaceSettings
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.WorkspaceFileProjectConfig
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.WorkspaceGitProjectConfig
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecRemovedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.GitSpecRemovedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.FileProjectStoreTestRequest
import com.orbitalhq.schemaServer.repositories.FileProjectTestResponse
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
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmissionException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.writeText

class FileWorkspaceConfigLoader(
   val configFilePath: Path,
   fallback: Config = ConfigFactory.systemEnvironment(),
   private val eventDispatcher: ProjectSpecLifecycleEventDispatcher,
   private val projectManager: ProjectLoaderManager,
   emitStateOnInit: Boolean = true,
   private val workspaceSettings: WorkspaceSettings = WorkspaceSettings()
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
      val oldFileSpecs = oldConfig?.fileConfigOrDefault(workspaceSettings)?.projects?.toSet() ?: emptySet()
      val newFileSpecs = workspaceConfig.fileConfigOrDefault(workspaceSettings).projects.toSet()
      val removedPackages = mutableListOf<PackageIdentifier>()
      val fileSpecsRemoved = Sets.difference(oldFileSpecs, newFileSpecs)
      fileSpecsRemoved.forEach { removedFileSpec ->
         removedPackages.addAll(getPackageIdentifierForTransport(removedFileSpec))
         eventDispatcher.fileRepositorySpecRemoved(FileSpecRemovedEvent(removedFileSpec))
      }
      val fileSpecsAdded = Sets.difference(newFileSpecs, oldFileSpecs)
      fileSpecsAdded.forEach { addedFileSpec ->
         eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(addedFileSpec, workspaceConfig.fileConfigOrDefault(workspaceSettings)))
      }

      val oldGitSpecs = oldConfig?.gitConfigOrDefault(workspaceSettings)?.repositories?.toSet() ?: emptySet()
      val newGitSpecs = workspaceConfig.gitConfigOrDefault(workspaceSettings).repositories.toSet()
      val gitSpecsRemoved = Sets.difference(oldGitSpecs, newGitSpecs)
      gitSpecsRemoved.forEach { removedGitSpec ->
         removedPackages.addAll(getPackageIdentifierForTransport(removedGitSpec))
         eventDispatcher.gitRepositorySpecRemoved(GitSpecRemovedEvent(removedGitSpec))
      }

      val addedGitSpecs = Sets.difference(newGitSpecs, oldGitSpecs)
      addedGitSpecs.forEach { addedGitSpec ->
         eventDispatcher.gitRepositorySpecAdded(GitSpecAddedEvent(addedGitSpec, workspaceConfig.gitConfigOrDefault(workspaceSettings)))
      }

      if (removedPackages.isNotEmpty()) {
         eventDispatcher.schemaSourceRemoved(removedPackages)
      }

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

   @VisibleForTesting
   public override fun invalidateCache() {
      super.invalidateCache()
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

   /**
    * Given a workspace file in /foo/bar/workspace.conf,
    * and paths declared relative (as baz/taxi.conf), will return
    * /foo/bar/baz/taxi.conf
    */
   private fun convertRelativePathsToAbsoluteFromWorkspaceFile(path: Path): Path {
      return if (path.isAbsolute) {
         path
      } else {
         configFilePath.toAbsolutePath().parent.resolve(path)
      }
   }

   /**
    * Given a workspace file in /foo/bar/workspace.conf,
    * and an absolute path declared as /foo/bar/baz/taxi.conf, will return
    * baz/taxi.conf
    *
    * Absolute paths which are not descendants of the workspace.conf file
    * are not changed
    */
   private fun convertDescendantPathsToRelative(path: Path): Path {
      return when {
         !path.isAbsolute -> path
         configFilePath.parent != null && path.startsWith(configFilePath.parent) -> {
            configFilePath.parent.relativize(path)
         }
         else -> path
      }
   }


   /**
    * Updates workspace paths before saving, to keep the workspace file portable.
    * see convertDescendantPathsToRelative
    */
   private fun makeAbsolutePathsRelative(original: WorkspaceConfig): WorkspaceConfig {
      return updatePaths(original, this::convertDescendantPathsToRelative)
   }

   /**
    * Updates workspace paths after loading, so they're absolute
    * see convertRelativePathsToAbsoluteFromWorkspaceFile
    */
   private fun resolveRelativePaths(original: WorkspaceConfig): WorkspaceConfig {
      return updatePaths(original, this::convertRelativePathsToAbsoluteFromWorkspaceFile)
   }

   private fun updatePaths(original: WorkspaceConfig, updater: (Path) -> Path): WorkspaceConfig {
      // MP: 06-Sep-24: Changed from original.file -> original.fileConfigOrDefault
      // We need to make newPaths relative to the location of the file, and this is the
      // only / best place to do it.
      val updatedFileConfig = original.fileConfigOrDefault(workspaceSettings).let { fileConfig ->
         val resolvedPaths = fileConfig.projects
            .map { packageSpec ->
               val updatedPath = updater(packageSpec.path)
               if (packageSpec.loader is TaxiPackageLoaderSpec) {
                  val packageMetadata = try {
                     // If we were passed a file, use it. Otherwise, if it's a dir, resolve taxi.conf file.
                     val pathToLoad = when {
                        !Files.exists(updatedPath) -> error("No file or directory exists at ${updatedPath.toFile().canonicalPath}")
                        updatedPath.isDirectory() -> updatedPath.resolve("taxi.conf")
                        updatedPath.isRegularFile() -> updatedPath
                        else -> error("Provided path is neither a file not a directory - not sure what to do")
                     }
                     TaxiPackageLoader(pathToLoad).load()?.toPackageMetadata()
                  } catch (e: Exception) {
                     val rootCause = Throwables.getRootCause(e)
                     logger.warn(e) { "Failed to read package metadata for project at $updatedPath  ${rootCause.message}" }
                     null
                  }
                  packageSpec.copy(path = updatedPath, packageIdentifier = packageMetadata?.identifier)
               } else {
                  packageSpec.copy(path = updatedPath)
               }
            }
         fileConfig.copy(
            projects = resolvedPaths,
            newProjectsPath = updater(fileConfig.newProjectsPath)
         )
      }
      val updatedGitConfig = original.gitConfigOrDefault(workspaceSettings).let { gitConfig ->
         val checkoutRoot = convertRelativePathsToAbsoluteFromWorkspaceFile(gitConfig.checkoutRoot)
         gitConfig.copy(checkoutRoot = checkoutRoot)
      }
      return original.copy(file = updatedFileConfig, git = updatedGitConfig)
   }

   override fun addFileSpec(fileSpec: FileProjectSpec): ModifyWorkspaceResponse {
      val current = this.typedConfig() // Don't call load, as we want the original, not the one we resolve paths with
      val currentFileConfig = current.file ?: WorkspaceFileProjectConfig()

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
      ).let { makeAbsolutePathsRelative(it) }
      save(updated)
      val fileSpecWithAbsolutePath =
         fileSpec.copy(path = convertRelativePathsToAbsoluteFromWorkspaceFile(fileSpec.path))
      eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(fileSpecWithAbsolutePath, updated.file!!))
      return ModifyWorkspaceResponse(ModifyProjectResponseStatus.Ok)
   }

   private fun verifyProjectExists(fileSpec: FileProjectSpec): PackageIdentifier {
      return when (fileSpec.loader) {
         is TaxiPackageLoaderSpec -> verifyTaxiProjectExists(fileSpec)
         is OpenApiPackageLoaderSpec -> verifyOpenApiProjectExists(fileSpec)
         else -> error("No package verification built for loader type ${fileSpec.loader::class.simpleName}")
      }

   }

   private fun verifyOpenApiProjectExists(fileSpec: FileProjectSpec): PackageIdentifier {
      require(fileSpec.path.exists()) { "No OpenAPI spec found at ${fileSpec.path}" }
      // What else do we need to check?
      return (fileSpec.loader as OpenApiPackageLoaderSpec).identifier
   }

   private fun verifyTaxiProjectExists(fileSpec: FileProjectSpec): PackageIdentifier {
      val projectPath = convertRelativePathsToAbsoluteFromWorkspaceFile(fileSpec.path)

      // TODO : Migrate this to TaxiPackageLoader.forDirectoryOrFilePath once available
      val project = if (projectPath.name.endsWith(".conf")) {
         TaxiPackageLoader.forPathToTaxiFile(projectPath).load()
      } else {
         TaxiPackageLoader.forDirectoryContainingTaxiFile(projectPath).load()
      }
      if (fileSpec.packageIdentifier != null && project.identifier.toVynePackageIdentifier() != fileSpec.packageIdentifier) {
         error("The provided package identifier (${fileSpec.packageIdentifier!!.id} does not match the package identifier found at ${fileSpec.path} - ${project.identifier.id}")
      }
      return project.identifier.toVynePackageIdentifier()
   }

   private fun createProjectIfNotExists(fileSpec: FileProjectSpec): PackageIdentifier {
      // We don't create openAPI projects
      if (fileSpec.loader is OpenApiPackageLoaderSpec) {
         return verifyOpenApiProjectExists(fileSpec)
      }

      val taxiProjectPath = convertRelativePathsToAbsoluteFromWorkspaceFile(fileSpec.path)

      if (!taxiProjectPath.createDirectories().exists()) {
         logger.warn { "Failed to create directory $taxiProjectPath for taxi project" }
         error("Failed to create directory $taxiProjectPath for taxi project")
      }
      val taxiPackageLoader = TaxiPackageLoader.forDirectoryContainingTaxiFile(taxiProjectPath)
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
         taxiProjectPath.resolve(project.sourceRoot).createDirectories()
      }

      return taxiPackageLoader.load().identifier.toVynePackageIdentifier()


   }

   override fun addGitSpec(gitSpec: GitProjectSpec): ModifyWorkspaceResponse {
      val current = this.typedConfig() // Don't call load, as we want the original, not the one we resolve paths with
      val currentGitConfig = current.git ?: WorkspaceGitProjectConfig()
      if (currentGitConfig.repositories.any { it.name == gitSpec.name }) {
         return ModifyWorkspaceResponse(
            ModifyProjectResponseStatus.Failed,
            "A git repository with the name ${gitSpec.name} already exists"
         )
      }

      // Customers use mono-repos, with lots of taxi projects inside the same repo.
      // So, this check doesn't make sense.
      // ORB-715
//      if (currentGitConfig.repositories.any { it.uri == gitSpec.uri }) {
//         return ModifyWorkspaceResponse(
//            ModifyProjectResponseStatus.Failed,
//            "A git repository already exists for ${gitSpec.uri}"
//         )
//      }

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

   override fun validateProjectExists(request: FileProjectStoreTestRequest): Mono<FileProjectTestResponse> {
      return Mono.fromCallable {
         val path = Paths.get(request.path)
         val projectHome = convertRelativePathsToAbsoluteFromWorkspaceFile(path)
         try {
            val project = TaxiPackageLoader.forDirectoryContainingTaxiFile(projectHome).load()
            FileProjectTestResponse(request.path, true, project.identifier.toVynePackageIdentifier())
         } catch (e: Exception) {
            logger.info { "Could not find a package at ${request.path} - maybe it doesn't exist? Error: ${e.message}" }
            FileProjectTestResponse(request.path, false, null)
         }
      }
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
