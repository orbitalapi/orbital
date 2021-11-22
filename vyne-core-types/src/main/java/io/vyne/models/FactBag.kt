package io.vyne.models

import io.vyne.query.AlwaysGoodSpec
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import java.util.concurrent.CopyOnWriteArrayList

class EmptyFactBag(private val list: List<TypedInstance> = emptyList()) : FactBag, Collection<TypedInstance> by list {
   private fun notSupported(): Nothing = throw RuntimeException("Not supported on an EmptyFactBag")
   override fun breadthFirstFilter(predicate: (TypedInstance) -> Boolean): List<TypedInstance> = emptyList()

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

   fun merge(other:FactBag):FactBag

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
