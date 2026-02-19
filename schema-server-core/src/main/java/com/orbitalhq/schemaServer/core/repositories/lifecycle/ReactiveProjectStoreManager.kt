package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.google.common.base.Throwables
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.ProjectLoaderManager
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.utils.files.ReactiveWatchingFileSystemMonitor
import mu.KotlinLogging
import reactor.core.Disposable
import java.nio.file.Path

/**
 * Watches spec lifecycle events, (eg., adding and removing new repositories)
 * and builds the corresponding repositories for them
 */
class ReactiveProjectStoreManager(
   private val fileRepoFactory: FileSystemPackageLoaderFactory,
   private val gitRepoFactory: GitSchemaPackageLoaderFactory,
   private val specEventSource: RepositorySpecLifecycleEventSource,
   private val eventDispatcher: ProjectStoreLifecycleEventDispatcher,
   private val repositoryEventSource: ProjectStoreLifecycleEventSource
) : ProjectLoaderManager, AutoCloseable {


   override fun getLoaderOrNull(packageIdentifier: PackageIdentifier): SchemaPackageTransport? {
      return loaders
         .firstOrNull { it.packageIdentifier.unversionedId == packageIdentifier.unversionedId }
   }

   override fun getLoader(packageIdentifier: PackageIdentifier): SchemaPackageTransport {
      val loader = getLoaderOrNull(packageIdentifier)
         ?: error("No file loader exists for package ${packageIdentifier.unversionedId}")

      if (!loader.isEditable()) {
         error("Package ${packageIdentifier.unversionedId} is not editable")
      }
      return loader
   }

   companion object {
      private val logger = KotlinLogging.logger {}
      fun testWithFileRepo(
         projectPath: Path? = null,
         isEditable: Boolean = false,
         eventSource: ProjectStoreLifecycleManager = ProjectStoreLifecycleManager()

      ): ReactiveProjectStoreManager {
         val manager = ReactiveProjectStoreManager(
            FileSystemPackageLoaderFactory(),
            GitSchemaPackageLoaderFactory(SchemaSourcesAdaptorFactory()),
            eventSource,
            eventSource,
            eventSource
         )
         if (projectPath != null) {
            manager.addLoader(
               FileSystemPackageLoader(
                  FileProjectSpec(projectPath, isEditable = isEditable),
                  TaxiSchemaSourcesAdaptor(),
                  ReactiveWatchingFileSystemMonitor(projectPath)
               )
            )
         }
         return manager
      }
   }


   private val _unhealthyLoaders = mutableMapOf<SchemaPackageTransport, LoaderStatus>()

   val unhealthyLoaders: List<UnhealthyLoaderWithStatus>
      get() {
         return _unhealthyLoaders.entries
            .map { (loader, status) -> UnhealthyLoaderWithStatus(loader, status) }
      }

   // key: Loader, value: Disposable of the statusFeed
   private val _loaders = mutableMapOf<SchemaPackageTransport, Disposable>()

   override val loaders: List<SchemaPackageTransport>
      get() = fileLoaders + gitLoaders
   val fileLoaders: List<FileSystemPackageLoader>
      get() {
         return _loaders
            .keys
            .filter { !_unhealthyLoaders.contains(it) }
            .filterIsInstance<FileSystemPackageLoader>()
            .toList()

      }
   val gitLoaders: List<GitSchemaPackageLoader>
      get() {
         return _loaders
            .keys
            .filter { !_unhealthyLoaders.contains(it) }
            .filterIsInstance<GitSchemaPackageLoader>()
            .toList()
      }

   private val fileSpecAddedEventsSubscription: Disposable
   private val fileSpecRemovedEventsSubscription: Disposable
   private val gitSpecAddedEventSubscription: Disposable
   private val gitSpecRemovedEventSubscription: Disposable
   private val repoRemovedEventSSubscription: Disposable

   init {
      fileSpecAddedEventsSubscription = consumeFileSpecAddedEvents()
      fileSpecRemovedEventsSubscription = consumeFileSpecRemovedEvents()
      gitSpecAddedEventSubscription = consumeGitSpecAddedEvents()
      gitSpecRemovedEventSubscription = consumeGitSpecRemovedEvents()
      repoRemovedEventSSubscription = consumeRepoRemovedEvents()
   }

   private fun addLoader(loader: SchemaPackageTransport) {
      val existingLoaders = this._loaders
         .filter { existingLoader ->
            try {
               // Note: This can throw an exception if the
               // existing loader is in an error state
               // If that happens, catch, log, and move on
               existingLoader.key.packageIdentifier == loader.packageIdentifier
            } catch (e: Exception) {
               val rootCause = Throwables.getRootCause(e)
               logger.warn { "A recoverable error occurred when validating for duplicates against ${existingLoader} : ${rootCause.message}. Cannot verify this isn't a duplicate project. Will continue" }
               false
            }

         }
      if (existingLoaders.isNotEmpty()) {
         logger.warn { "At attempt was made to add a duplicate loader - ${existingLoaders.size} loaders already exist for project ${loader.packageIdentifier.id}" }
         return
      }

      val stateSubscription = loader.loaderStatus.subscribe { status ->
         when (status.state) {
            LoaderStatus.LoaderState.ERROR -> _unhealthyLoaders[loader] = status
            LoaderStatus.LoaderState.WARNING -> _unhealthyLoaders[loader] = status
            else -> _unhealthyLoaders.remove(loader)
         }
      }
      this._loaders[loader] = stateSubscription
   }

   private fun removeLoader(loader: SchemaPackageTransport) {
      loader.stop()
      this._loaders.remove(loader)?.dispose()
      this._unhealthyLoaders.remove(loader)
   }

   private fun consumeRepoRemovedEvents(): Disposable {

      // Triggered when the user removes a reppository from the UI.
      // A bit of hoop jumping here as we dispatch the packages affected, rather than the loaders.
      // Also, this needs a test.

      return repositoryEventSource.sourcesRemoved.subscribe { packages ->
         val fileLoadersToRemove = fileLoaders.filter { fileLoader -> packages.contains(fileLoader.packageIdentifier) }
         if (fileLoadersToRemove.isNotEmpty()) {
            fileLoadersToRemove.forEach { removeLoader(it) }
            logger.info { "Removed ${fileLoadersToRemove.size} file loaders" }
         }
         val gitLoadersToRemove = gitLoaders.filter { gitLoader -> packages.contains(gitLoader.packageIdentifier) }
         if (gitLoadersToRemove.isNotEmpty()) {
            logger.info { "Removed ${gitLoadersToRemove.size} file loaders" }
            gitLoadersToRemove.forEach { removeLoader(it) }
         }
      }
   }

   private fun consumeGitSpecAddedEvents(): Disposable {
      return specEventSource.gitSpecAdded.map { event ->
         gitRepoFactory.build(event.config, event.spec)
      }.subscribe { loader ->
         addLoader(loader)
         eventDispatcher.gitProjectStoreAdded(loader)
      }
   }

   private fun consumeFileSpecAddedEvents(): Disposable {
      return specEventSource.fileSpecAdded.map { event ->
         fileRepoFactory.build(
            event.config, event.spec
         )
      }.subscribe { loader: FileSystemPackageLoader ->
         addLoader(loader)
         eventDispatcher.fileProjectStoreAdded(loader)
      }

      //specEventSource.fileSpecAdded
   }

   /**
    * These are events when the file spec is removed by manually editing the
    * workspace.conf file
    */
   private fun consumeFileSpecRemovedEvents(): Disposable {
      return specEventSource.fileSpecRemoved.subscribe { event ->
         logger.info { "Received event that file spec at ${event.spec.path} (with package of ${event.spec.packageIdentifier?.id} has been removed. Removing any loaders" }
         // Don't use fileLoaders, as that excludes invalid configs
         _loaders.keys.filterIsInstance<FileSystemPackageLoader>()
            .filter { loader ->
               // can't use packageIdentifier here, as it's not required that one has been defined
               loader.config.path == event.spec.path
            }.forEach { loader -> removeLoader(loader) }
      }
   }

   /**
    * These are events when the git spec is removed by manually editing the
    * workspace.conf file (or modified, which generates a remove + add event)
    */
   private fun consumeGitSpecRemovedEvents(): Disposable {
      return specEventSource.gitSpecRemoved.subscribe { event ->
         logger.info { "Received event that git spec ${event.spec.name} at ${event.spec.uri} has been removed. Removing loaders and cleaning up working directory" }

         // Find matching git loaders (don't use gitLoaders property as it excludes unhealthy ones)
         val matchingLoaders = _loaders.keys.filterIsInstance<GitSchemaPackageLoader>()
            .filter { loader -> loader.config == event.spec }

         matchingLoaders.forEach { loader ->
            val workingDir = loader.workingDir
            removeLoader(loader)
            deleteGitWorkingDirectory(workingDir)
         }
      }
   }

   /**
    * Safely deletes a git working directory after a git spec is removed
    */
   private fun deleteGitWorkingDirectory(workingDir: Path) {
      try {
         if (!java.nio.file.Files.exists(workingDir)) {
            logger.debug { "Working directory $workingDir does not exist, skipping deletion" }
            return
         }

         // Safety check: verify it's a git directory
         val gitDir = workingDir.resolve(".git")
         if (!java.nio.file.Files.exists(gitDir)) {
            logger.warn { "Directory $workingDir does not contain a .git folder, skipping deletion for safety" }
            return
         }

         logger.info { "Deleting git working directory at $workingDir" }
         workingDir.toFile().deleteRecursively()
         logger.info { "Successfully deleted git working directory at $workingDir" }

      } catch (e: Exception) {
         // Non-fatal: Log warning but don't throw
         // Directory will be handled on next clone attempt
         logger.warn(e) { "Failed to delete git working directory at $workingDir: ${e.message}. This will not prevent the new clone." }
      }
   }

   override val editableLoaders: List<SchemaPackageTransport>
      get() {
         return loaders.filter { it.isEditable() }
      }

   override fun close() {
      fileSpecAddedEventsSubscription.dispose()
      fileSpecRemovedEventsSubscription.dispose()
      gitSpecAddedEventSubscription.dispose()
      gitSpecRemovedEventSubscription.dispose()
      repoRemovedEventSSubscription.dispose()
   }

}

data class UnhealthyLoaderWithStatus(
   val loaderDescription: String,
   val status: LoaderStatus
) {
   constructor(loader: SchemaPackageTransport, status: LoaderStatus) : this(loader.description, status)
}
