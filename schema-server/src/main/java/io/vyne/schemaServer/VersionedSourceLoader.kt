package io.vyne.schemaServer

import io.vyne.VersionedSource

interface VersionedSourceLoader {

   val identifier: String
   fun loadVersionedSources(incrementVersion: Boolean): List<VersionedSource>
}
