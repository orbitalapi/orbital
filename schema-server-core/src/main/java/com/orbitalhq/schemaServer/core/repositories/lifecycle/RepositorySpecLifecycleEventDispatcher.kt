package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.PackageIdentifier


/**
 * Ligthweight interface which emits messages when the *spec* for a
 * repository has been changed.
 */
interface RepositorySpecLifecycleEventDispatcher {
   fun fileRepositorySpecAdded(spec: FileSpecAddedEvent)
   fun gitRepositorySpecAdded(spec: GitSpecAddedEvent)

   fun schemaSourceRemoved(packages: List<PackageIdentifier>)
}

object NoOpRepositorySpecLifecycleEventDispatcher : RepositorySpecLifecycleEventDispatcher {
   override fun fileRepositorySpecAdded(spec: FileSpecAddedEvent) {
   }

   override fun gitRepositorySpecAdded(spec: GitSpecAddedEvent) {
   }

   override fun schemaSourceRemoved(packages: List<PackageIdentifier>) {
   }
}
