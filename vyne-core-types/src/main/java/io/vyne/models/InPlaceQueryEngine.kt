package io.vyne.models

import io.vyne.models.facts.ScopedFact
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
   suspend fun findType(
      type: Type,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      permittedStrategy: PermittedQueryStrategies = PermittedQueryStrategies.EVERYTHING
   ): Flow<TypedInstance> {
      return this.findType(type, permittedStrategy)
         .filter { spec.isValid(it) }
   }

   suspend fun findType(
      type: Type,
      permittedStrategy: PermittedQueryStrategies = PermittedQueryStrategies.EVERYTHING
   ): Flow<TypedInstance>

   fun only(fact: TypedInstance, scopedFacts: List<ScopedFact> = emptyList()): InPlaceQueryEngine {
      return only(listOf(fact), scopedFacts)
   }

   fun only(facts: List<TypedInstance>, scopedFacts: List<ScopedFact> = emptyList()): InPlaceQueryEngine

   fun withAdditionalFacts(facts: List<TypedInstance>, scopedFacts: List<ScopedFact>): InPlaceQueryEngine
}

/**
 * A light-weight version of the PermittedQueryStrategyPredicate,
 * which we don't want to leak into the core library.
 */
enum class PermittedQueryStrategies {
   EVERYTHING,
   EXCLUDE_BUILDER,
   EXCLUDE_BUILDER_AND_MODEL_SCAN
}
