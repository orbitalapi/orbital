package io.vyne.schemaServer.core.repositories

import io.vyne.PackageIdentifier
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoader

/**
 * Facilitates CRUD style operations on schema repositories.
 *
 * Only runtime implementation (ReactiveRepositoryManager)
 * but interface exists to help facilitate testing
 */
interface SchemaRepositoryManager {
   val editableLoaders: List<FileSystemPackageLoader>

   fun getLoader(packageIdentifier: PackageIdentifier): SchemaPackageTransport
}


class SimpleRepositoryManager(initialLoaders:List<FileSystemPackageLoader> = emptyList()) : SchemaRepositoryManager {
   private val loaders = mutableListOf<FileSystemPackageLoader>()

   init {
       loaders.addAll(initialLoaders)
   }

   override fun getLoader(packageIdentifier: PackageIdentifier): SchemaPackageTransport {
      return loaders.single { it.packageIdentifier == packageIdentifier }
   }

   override val editableLoaders: List<FileSystemPackageLoader>
      get() {
         return loaders.filter { it.isEditable }
      }
}
