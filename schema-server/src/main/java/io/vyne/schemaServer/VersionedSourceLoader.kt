package io.vyne.schemaServer

import io.vyne.VersionedSource

interface VersionedSourceLoader {
   fun getSourcesFromFileSystem(incrementVersion: Boolean): List<VersionedSource>
}
