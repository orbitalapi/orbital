package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.api.SchemaPackageTransport
import com.orbitalhq.schema.consumer.SchemaTransportProvider
import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.utils.files.ReactiveWatchingFileSystemMonitor
import mu.KotlinLogging
import java.nio.file.Path

/**
 * Watches spec lifecycle events, (eg., adding and removing new repositories)
 * and builds the corresponding repositories for them
 */
class ReactiveRepositoryManager(
   private val fileRepoFactory: FileSystemPackageLoaderFactory,
   private val gitRepoFactory: GitSchemaPackageLoaderFactory,
   private val specEventSource: RepositorySpecLifecycleEventSource,
   private val eventDispatcher: RepositoryLifecycleEventDispatcher,
   private val repositoryEventSource: RepositoryLifecycleEventSource
) : SchemaTransportProvider {

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
         eventSource: RepositoryLifecycleManager = RepositoryLifecycleManager()

      ): ReactiveRepositoryManager {
         val manager = ReactiveRepositoryManager(
            FileSystemPackageLoaderFactory(),
            GitSchemaPackageLoaderFactory(SchemaSourcesAdaptorFactory()),
            eventSource,
            eventSource,
            eventSource
         )
         if (projectPath != null) {
            manager._fileLoaders.add(
               FileSystemPackageLoader(
                  FileSystemPackageSpec(projectPath, isEditable = isEditable),
                  TaxiSchemaSourcesAdaptor(),
                  ReactiveWatchingFileSystemMonitor(projectPath)
               )
            )
         }
         return manager
      }
   }

   private val _fileLoaders = mutableListOf<FileSystemPackageLoader>()

   private val _gitLoaders = mutableListOf<GitSchemaPackageLoader>()

   val loaders: List<SchemaPackageTransport>
      get() = _fileLoaders + _gitLoaders
   val fileLoaders: List<FileSystemPackageLoader>
      get() {
         return _fileLoaders.toList()
      }
   val gitLoaders: List<GitSchemaPackageLoader>
      get() {
         return _gitLoaders.toList()
      }

   init {
      consumeFileSpecAddedEvents()
      consumeGitSpecAddedEvents()
      consumeRepoRemovedEvents()
   }

   private fun consumeRepoRemovedEvents() {

      // Triggered when the user removes a reppository from the UI.
      // A bit of hoop jumping here as we dispatch the packages affected, rather than the loaders.
      // Also, this needs a test.

      repositoryEventSource.sourcesRemoved.subscribe { packages ->
         val fileLoadersToRemove = _fileLoaders.filter { fileLoader -> packages.contains(fileLoader.packageIdentifier) }
         if (fileLoadersToRemove.isNotEmpty()) {
            _fileLoaders.removeAll(fileLoadersToRemove)
            logger.info { "Removed ${fileLoadersToRemove.size} file loaders" }
         }
         val gitLoadersToRemove = gitLoaders.filter { gitLoader -> packages.contains(gitLoader.packageIdentifier) }
         if (gitLoadersToRemove.isNotEmpty()) {
            logger.info { "Removed ${gitLoadersToRemove.size} file loaders" }
            _gitLoaders.removeAll(gitLoadersToRemove)
         }
      }
   }

   private fun consumeGitSpecAddedEvents() {
      specEventSource.gitSpecAdded.map { event ->
         gitRepoFactory.build(event.config, event.spec)
      }.subscribe { loader ->
         _gitLoaders.add(loader)
         eventDispatcher.gitRepositoryAdded(loader)
      }
   }

   private fun consumeFileSpecAddedEvents() {
      specEventSource.fileSpecAdded.map { event ->
         fileRepoFactory.build(
            event.config, event.spec
         )
      }.subscribe { loader: FileSystemPackageLoader ->
         _fileLoaders.add(loader)
         eventDispatcher.fileRepositoryAdded(loader)
      }

      specEventSource.fileSpecAdded
   }

   val editableLoaders: List<FileSystemPackageLoader>
      get() {
         return _fileLoaders.filter { it.isEditable() }
      }

}
