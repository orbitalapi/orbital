package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader

/**
 * Lightweight interface which allows emission of lifecycle events
 * related to the creation and modification of source repositories
 */
interface RepositoryLifecycleEventDispatcher {
   fun fileRepositoryAdded(repository: FileSystemPackageLoader)
   fun gitRepositoryAdded(repository: GitSchemaPackageLoader)

}
