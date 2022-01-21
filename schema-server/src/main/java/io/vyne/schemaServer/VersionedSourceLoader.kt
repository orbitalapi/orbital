package io.vyne.schemaServer

import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import io.vyne.schemaServer.file.SourcesChangedMessage
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
   fun loadVersionedSources(forceVersionIncrement: Boolean = false, cachedValuePermissible:Boolean = true): VersionedSourceSubmission
}

interface UpdatingVersionedSourceLoader : VersionedSourceLoader {
   val sourcesChanged: Flux<SourcesChangedMessage>
}
