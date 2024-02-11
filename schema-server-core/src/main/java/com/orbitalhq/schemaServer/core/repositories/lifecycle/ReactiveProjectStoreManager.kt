package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.ConfigSourceWriter
import com.orbitalhq.config.ConfigSourceWriterProvider
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.schema.publisher.ProjectLoaderManager
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.utils.files.ReactiveWatchingFileSystemMonitor
import mu.KotlinLogging
import reactor.core.Disposable
import java.nio.file.Path
import kotlin.io.path.isRegularFile

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

   override val loaders: List<SchemaPackageTransport>
      get() = _fileLoaders + _gitLoaders
   val fileLoaders: List<FileSystemPackageLoader>
      get() {
         return _fileLoaders.toList()
      }
   val gitLoaders: List<GitSchemaPackageLoader>
      get() {
         return _gitLoaders.toList()
      }

   private val fileSpecAddedEventsSubscription: Disposable
   private val gitSpecAddedEventSubscription: Disposable
   private val repoRemovedEventSSubscription: Disposable

   init {
      fileSpecAddedEventsSubscription = consumeFileSpecAddedEvents()
      gitSpecAddedEventSubscription = consumeGitSpecAddedEvents()
      repoRemovedEventSSubscription = consumeRepoRemovedEvents()
   }

   private fun consumeRepoRemovedEvents(): Disposable {

      // Triggered when the user removes a reppository from the UI.
      // A bit of hoop jumping here as we dispatch the packages affected, rather than the loaders.
      // Also, this needs a test.

     return repositoryEventSource.sourcesRemoved.subscribe { packages ->
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

   private fun consumeGitSpecAddedEvents(): Disposable {
     return specEventSource.gitSpecAdded.map { event ->
         gitRepoFactory.build(event.config, event.spec)
      }.subscribe { loader ->
         _gitLoaders.add(loader)
         eventDispatcher.gitProjectStoreAdded(loader)
      }
   }

   private fun consumeFileSpecAddedEvents(): Disposable {
      return specEventSource.fileSpecAdded.map { event ->
         fileRepoFactory.build(
            event.config, event.spec
         )
      }.subscribe { loader: FileSystemPackageLoader ->
         _fileLoaders.add(loader)
         eventDispatcher.fileProjectStoreAdded(loader)
      }

      //specEventSource.fileSpecAdded
   }

   override val editableLoaders: List<FileSystemPackageLoader>
      get() {
         return _fileLoaders.filter { it.isEditable() }
      }

   override fun close() {
      fileSpecAddedEventsSubscription.dispose()
      gitSpecAddedEventSubscription.dispose()
      repoRemovedEventSSubscription.dispose()
   }

}
