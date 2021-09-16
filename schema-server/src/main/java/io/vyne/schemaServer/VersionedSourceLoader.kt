package io.vyne.schemaServer

import io.vyne.VersionedSource

interface VersionedSourceLoader {

   val identifier: String

   /**
    * Loads the versioned sources.
    * If forceVersionIncrement is true, then the pre-release version is incremented.
    * Otherwise, the default behaviour of the VersionedSourceLoader is applied.
    */
   fun loadVersionedSources(forceVersionIncrement: Boolean = false): List<VersionedSource>
}
