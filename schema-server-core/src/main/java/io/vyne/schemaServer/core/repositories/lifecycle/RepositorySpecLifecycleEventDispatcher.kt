package io.vyne.schemaServer.core.repositories.lifecycle

import io.vyne.PackageIdentifier


/**
 * Ligthweight interface which emits messages when the *spec* for a
 * repository has been changed.
 */
interface RepositorySpecLifecycleEventDispatcher {
   fun fileRepositorySpecAdded(spec: FileSpecAddedEvent)
   fun gitRepositorySpecAdded(spec: GitSpecAddedEvent)

   fun schemaSourceRemoved(packages: List<PackageIdentifier>)
}
