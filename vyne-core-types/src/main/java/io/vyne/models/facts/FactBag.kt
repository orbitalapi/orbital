package io.vyne.models.facts

import io.vyne.models.TypedInstance
import io.vyne.query.AlwaysGoodSpec
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.accessors.Argument
import lang.taxi.accessors.ProjectionFunctionScope

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
    * Exposes facts that were defined with a specific declared scope.
    * Normally, this is at the start of a projection
    * (eg:
    * find { Foo } as (foo:Foo) { <------ That's a scope.
    */
   val scopedFacts: List<ScopedFact>

   fun getScopedFact(scope: Argument): ScopedFact {
      return getScopedFactOrNull(scope) ?:
         error("No scope of ${scope.name} exists in this FactBag")
   }

   fun getScopedFactOrNull(scope: Argument): ScopedFact? {
      return scopedFacts.firstOrNull { it.scope == scope }
   }

   fun rootAndScopedFacts(): List<TypedInstance> {
      return rootFacts() + scopedFacts.map { it.fact }
   }

   override fun contains(element: TypedInstance): Boolean {
      return rootFacts().contains(element) || scopedFacts.any { it.fact == element }
   }

   override fun containsAll(elements: Collection<TypedInstance>): Boolean {
      return elements.all { contains(it) }
   }

   override fun isEmpty(): Boolean = rootAndScopedFacts().isEmpty()
   override val size: Int
      get() {
         return rootAndScopedFacts().size
      }

   override fun iterator(): Iterator<TypedInstance> {
      return rootAndScopedFacts().iterator()
   }

   /**
    * Returns the facts that were provided top-level only.
    * Other facts may be available by traversing the trees of these facts
    */
   fun rootFacts(): List<TypedInstance>

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
   fun excluding(facts: Set<TypedInstance>): FactBag

   //   fun firstOrNull(predicate: (TypedInstance) -> Boolean): TypedInstance?
//   fun filter(predicate: (TypedInstance) -> Boolean): List<TypedInstance>
   fun breadthFirstFilter(
      strategy: FactDiscoveryStrategy,
      shouldGoDeeperPredicate: FactMapTraversalStrategy,
      matchingPredicate: (TypedInstance) -> Boolean
   ): List<TypedInstance>

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
