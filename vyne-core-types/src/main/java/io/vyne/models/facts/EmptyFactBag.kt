package io.vyne.models.facts

import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.Type

class EmptyFactBag(private val list: List<TypedInstance> = emptyList()) : FactBag, Collection<TypedInstance> by list {
   private fun notSupported(): Nothing = throw RuntimeException("Not supported on an EmptyFactBag")
   override fun breadthFirstFilter(
       strategy: FactDiscoveryStrategy,
       shouldGoDeeperPredicate: FactMapTraversalStrategy,
       matchingPredicate: (TypedInstance) -> Boolean
   ): List<TypedInstance> = emptyList()

   override val scopedFacts: List<ScopedFact> = emptyList()

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
