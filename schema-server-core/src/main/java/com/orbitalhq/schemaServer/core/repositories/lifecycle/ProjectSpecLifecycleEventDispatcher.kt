package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.PackageIdentifier


/**
 * Ligthweight interface which emits messages when the *spec* for a
 * repository has been changed.
 */
interface ProjectSpecLifecycleEventDispatcher {
   fun fileRepositorySpecAdded(spec: FileSpecAddedEvent)
   fun fileRepositorySpecRemoved(spec: FileSpecRemovedEvent)

   fun gitRepositorySpecAdded(spec: GitSpecAddedEvent)
   fun gitRepositorySpecRemoved(spec: GitSpecRemovedEvent)

   fun schemaSourceRemoved(packages: List<PackageIdentifier>)
}

object NoOpProjectSpecLifecycleEventDispatcher : ProjectSpecLifecycleEventDispatcher {
   override fun fileRepositorySpecAdded(spec: FileSpecAddedEvent) {
   }

   override fun fileRepositorySpecRemoved(spec: FileSpecRemovedEvent) {
   }

   override fun gitRepositorySpecAdded(spec: GitSpecAddedEvent) {
   }

   override fun gitRepositorySpecRemoved(spec: GitSpecRemovedEvent) {
   }

   override fun schemaSourceRemoved(packages: List<PackageIdentifier>) {
   }
}
