package io.vyne.schemaServer.core.repositories.lifecycle

import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.git.GitRepositoryConfig

/**
 * Ligthweight interface which emits messages when the *spec* for a
 * repository has been changed.
 */
interface RepositorySpecLifecycleEventDispatcher {
   fun fileRepositorySpecAdded(spec: FileSpecAddedEvent)
   fun gitRepositorySpecAdded(spec: GitSpecAddedEvent)
}
