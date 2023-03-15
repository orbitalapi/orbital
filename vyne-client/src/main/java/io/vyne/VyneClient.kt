package io.vyne

import io.vyne.schemas.Schema
import reactor.core.publisher.Flux

inline fun <reified T : Any> VyneClient.query(query: String): Flux<T> {
   return queryWithType(query, T::class.java)
}

interface VyneClient {
   fun <T : Any> queryWithType(query: String, type: Class<T>): Flux<T>
}

interface VyneClientWithSchema : VyneClient {
   val schema: Schema
}
