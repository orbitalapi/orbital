package io.vyne.schemaServer.core.repositories.lifecycle

import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schemaServer.core.file.SourcesChangedMessage
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
}
