package io.vyne.models

import io.vyne.query.AlwaysGoodSpec
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.Schema
import io.vyne.schemas.Type


/**
 * A FactBag is a collection of Facts (ie., TypedInstances) for search purposes.
 * It's responsible for providing rich search capability, and caching searches for facts
 * to optimize search time.
 */
interface FactBag : Collection<TypedInstance> {
   companion object {
      fun of(facts: List<TypedInstance>, schema: Schema): FactBag {
         return CopyOnWriteFactBag(facts, schema)
      }
   }

   //   fun firstOrNull(predicate: (TypedInstance) -> Boolean): TypedInstance?
//   fun filter(predicate: (TypedInstance) -> Boolean): List<TypedInstance>
   fun breadthFirstFilter(predicate: (TypedInstance) -> Boolean): List<TypedInstance>
   fun addFact(fact: TypedInstance): FactBag
   fun addFacts(facts: Collection<TypedInstance>): FactBag
   fun hasFactOfType(
      type: Type,
      strategy: FactDiscoveryStrategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): Boolean

   fun getFact(
      type: Type,
      strategy: FactDiscoveryStrategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): TypedInstance

   fun getFactOrNull(
      type: Type,
      strategy: FactDiscoveryStrategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): TypedInstance?

   fun getFactOrNull(
      search: FactSearch,
   ): TypedInstance?

   fun hasFact(
      search: FactSearch
   ): Boolean
}
