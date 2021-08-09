package io.vyne.schemaServer

import io.vyne.VersionedSource

interface VersionedSourceLoader {
   fun loadVersionedSources(incrementVersion: Boolean): List<VersionedSource>
}
