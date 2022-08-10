package io.vyne.schemaServer.core

import io.vyne.SourcePackage
import io.vyne.schemaServer.core.file.SourcesChangedMessage
import reactor.core.publisher.Flux

interface VersionedSourceLoader {

   val identifier: String

   /**
    * Loads the versioned sources.
    * If forceVersionIncrement is true, then the pre-release version is incremented.
    * Otherwise, the default behaviour of the VersionedSourceLoader is applied.
    *
    * Implementations should be aware they may be called at any time, and frequently, as other
    * sources change.  Therefore, if the load operation is expensive, they should consider caching, and
    * implementing their own cache invalidation strategy
    */
   fun loadSourcePackage(forceVersionIncrement: Boolean = false, cachedValuePermissible:Boolean = true): SourcePackage
}

interface UpdatingVersionedSourceLoader : VersionedSourceLoader {
   val sourcesChanged: Flux<SourcesChangedMessage>
}
