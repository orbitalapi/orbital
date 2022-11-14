package io.vyne.models.facts

import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.query.AlwaysGoodSpec
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.Schema
import io.vyne.schemas.Type

class EmptyFactBag(private val list: List<TypedInstance> = emptyList()) : FactBag, Collection<TypedInstance> by list {
   private fun notSupported(): Nothing = throw RuntimeException("Not supported on an EmptyFactBag")
   override fun breadthFirstFilter(
      strategy: FactDiscoveryStrategy,
      shouldGoDeeperPredicate: FactMapTraversalStrategy,
      matchingPredicate: (TypedInstance) -> Boolean
   ): List<TypedInstance> = emptyList()

   override fun rootFacts(): List<TypedInstance> = emptyList()

   override fun addFact(fact: TypedInstance): FactBag = notSupported()

   override fun addFacts(facts: Collection<TypedInstance>): FactBag = notSupported()

   override fun hasFactOfType(type: Type, strategy: FactDiscoveryStrategy, spec: TypedInstanceValidPredicate): Boolean =
      false

   override fun getFact(type: Type, strategy: FactDiscoveryStrategy, spec: TypedInstanceValidPredicate): TypedInstance =
      TypedNull.create(type)

   override fun getFactOrNull(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? = null

   override fun getFactOrNull(search: FactSearch): TypedInstance? = null

   override fun hasFact(search: FactSearch): Boolean = false

   override fun merge(other: FactBag): FactBag {
      return if (other is EmptyFactBag) {
         this
      } else {
         // I suspect I'll regret this...
         other.merge(this)
      }
   }

   override fun merge(fact: TypedInstance): FactBag {
      // Hmm... not sure how to do this, since constructing a FactBag
      // requires a schema.
      // We
      TODO("This isn't implemented, as we need a Schema instance.")
   }

   override fun excluding(facts: Set<TypedInstance>): FactBag {
      TODO("Not yet implemented")
   }
}

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

      fun empty(): FactBag {
         return EmptyFactBag()
      }
   }

   /**
    * Returns the facts that were provided top-level only.
    * Other facts may be available by traversing the trees of these facts
    */
   fun rootFacts():List<TypedInstance>

   fun merge(other: FactBag): FactBag

   /**
    * Returns a new FactBag, containing the facts from this instance,
    * plus the fact provided.
    *
    * The current instance is unchanged.
    */
   fun merge(fact: TypedInstance): FactBag

   /**
    * Returns a new FactBack, containing the current facts, but
    * without the excluded facts
    *
    * The current instance is unchanged
    */
   fun excluding(facts:Set<TypedInstance>): FactBag

   //   fun firstOrNull(predicate: (TypedInstance) -> Boolean): TypedInstance?
//   fun filter(predicate: (TypedInstance) -> Boolean): List<TypedInstance>
   fun breadthFirstFilter(strategy: FactDiscoveryStrategy, shouldGoDeeperPredicate: FactMapTraversalStrategy,  matchingPredicate: (TypedInstance) -> Boolean): List<TypedInstance>

   /**
    * A mutating operation.  Adds a fact to the current fact bag.
    * The current, mutated fact bag is returned for convenience
    */
   fun addFact(fact: TypedInstance): FactBag

   /**
    * A mutating operation.  Adds a fact to the current fact bag.
    * The current, mutated fact bag is returned for convenience
    */
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
