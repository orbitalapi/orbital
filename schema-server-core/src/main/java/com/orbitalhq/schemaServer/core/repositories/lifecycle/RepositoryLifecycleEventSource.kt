package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schemaServer.core.file.SourcesChangedMessage
import reactor.core.publisher.Flux

/**
 * Lightweight interface which provides access to various
 * lifecycle events related to the creation and modification of source repositories
 */
interface RepositoryLifecycleEventSource {
   val repositoryAdded: Flux<SchemaPackageTransport>

   /**
    * A combined flux of all sourcesChanged events from all repositories
    * currently active with the system
    */
   val sourcesChanged: Flux<SourcesChangedMessage>

   val sourcesRemoved: Flux<List<PackageIdentifier>>
}
