package io.vyne.schemaServer.core.repositories.lifecycle

import io.vyne.PackageIdentifier
import io.vyne.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import io.vyne.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.packages.*
import io.vyne.schemaServer.core.git.GitSchemaPackageLoader
import io.vyne.schemaServer.core.git.GitSchemaPackageLoaderFactory
import mu.KotlinLogging
import java.nio.file.Path

/**
 * Watches spec lifecycle events, and creates
 * the corresponding repositories for them
 */
class ReactiveRepositoryManager(
   private val fileRepoFactory: FileSystemPackageLoaderFactory,
   private val gitRepoFactory: GitSchemaPackageLoaderFactory,
   private val eventSource: RepositorySpecLifecycleEventSource,
   private val eventDispatcher: RepositoryLifecycleEventDispatcher
) {
   fun getLoader(packageIdentifier: PackageIdentifier): FileSystemPackageLoader {
      val loader = fileLoaders
         .firstOrNull { it.packageIdentifier.unversionedId == packageIdentifier.unversionedId }
         ?: error("No file loader exists for package ${packageIdentifier.unversionedId}")

      if (!loader.editable) {
         error("Package ${packageIdentifier.unversionedId} is not editable")
      }
      return loader
   }

   companion object {
      fun testWithFileRepo(
         projectPath: Path? = null,
         editable: Boolean = false,
         eventSource: RepositoryLifecycleManager = RepositoryLifecycleManager()
      ): ReactiveRepositoryManager {
         val manager = ReactiveRepositoryManager(
            FileSystemPackageLoaderFactory(),
            GitSchemaPackageLoaderFactory(SchemaSourcesAdaptorFactory()),
            eventSource,
            eventSource
         )
         if (projectPath != null) {
            manager._fileLoaders.add(
               FileSystemPackageLoader(
                  FileSystemPackageSpec(projectPath, editable = editable),
                  TaxiSchemaSourcesAdaptor(),
                  ReactiveWatchingFileSystemMonitor(projectPath)
               )
            )
         }
         return manager
      }
   }

   private val logger = KotlinLogging.logger {}
   private val _fileLoaders = mutableListOf<FileSystemPackageLoader>()

   private val _gitLoaders = mutableListOf<GitSchemaPackageLoader>()
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
   }

   private fun consumeGitSpecAddedEvents() {
      eventSource.gitSpecAdded.map { event ->
         gitRepoFactory.build(event.config, event.spec)
      }.subscribe { loader ->
         _gitLoaders.add(loader)
         eventDispatcher.gitRepositoryAdded(loader)
      }
   }

   private fun consumeFileSpecAddedEvents() {
      eventSource.fileSpecAdded.map { event ->
         fileRepoFactory.build(
            event.config, event.spec
         )
      }.subscribe { loader: FileSystemPackageLoader ->
         _fileLoaders.add(loader)
         eventDispatcher.fileRepositoryAdded(loader)
      }
   }

   val editableLoaders: List<FileSystemPackageLoader>
      get() {
         return _fileLoaders.filter { it.editable }
      }
}