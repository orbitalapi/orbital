package com.orbitalhq.query.caching

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Schema
import reactor.core.publisher.Mono

/**
 * Returns a Cache Store for the provided key - if one is available, otherwise
 * returns null.
 */
interface StateStoreProvider {
   // Note: Be sure to pass a schema from the query context (not from the
   // schema store), as we want to ensure inline types declared by the query are available.
   fun getCacheStore(connectionName: String, key: String, schema: Schema):StateStore?
}

/**
 * Intended to provide interim state when doing things like merging multiple streams.
 *
 */
interface StateStore {
   /**
    * Updates the stored instance, copying the non-null values from
    * the provided instance onto the stored instance, and returns the result.
    *
    * - @Id annotated fields are used for matching
    * - If the @Id annotated field is null, then the original instance is returned unmodified
    *
    */
   fun mergeNotNullValues(typedInstance: TypedInstance): Mono<TypedInstance>
}


