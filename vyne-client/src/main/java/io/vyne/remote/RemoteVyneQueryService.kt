package io.vyne.remote

import io.vyne.schemas.Schema
import reactor.core.publisher.Flux

/**
 * An interface describing a service that can be used to query a remote Vyne instance. The implementation can choose
 * the specifics like an implementation using Spring WebClient and supporting streaming queries. An instance of this n
 * needs to be passed for the RemoteVyneClient so that it can execute the queries.
 */
interface RemoteVyneQueryService {
   fun <T : Any> queryWithType(query: String, type: Class<T>, schema: Schema? = null): Flux<T>
}
