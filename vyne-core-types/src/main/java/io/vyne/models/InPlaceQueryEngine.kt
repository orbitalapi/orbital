package io.vyne.models

import io.vyne.query.AlwaysGoodSpec
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.Type
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

/**
 * This is a lightweight version of the API exposed by Vyne's
 * QueryContext / QueryEngine.
 *
 * That API has high tech debt, and lots of attributes that are deprecated, but that
 * make it hard to move from Vyne to VyneCore.
 *
 * This interface provides a lightweight way of running queries, without access to the full
 * power of Vyne.  Used in places like TypedObjectFactory, when we want to be able to do
 * in-place discovery.
 *
 * Crucially, this interface doesn't provide a way to mutate the scope of the query.
 * It's  (currently) expected that the implementor of this interface has enough context to understand
 * what's going on, so can perform scoping on behalf of the query caller.  However, this design
 * choice isn't carefully considered at this point, and may need to chang.e
 */
interface InPlaceQueryEngine {
   suspend fun findType(type:Type, spec: TypedInstanceValidPredicate = AlwaysGoodSpec): Flow<TypedInstance> {
      return this.findType(type)
         .filter { spec.isValid(it) }
   }
   suspend fun findType(type:Type): Flow<TypedInstance>

}
