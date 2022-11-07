package io.vyne.models.facts

import com.google.common.collect.Iterators
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.Schema
import io.vyne.schemas.Type

/**
 * Allows for segreated searches of facts.
 *
 * Initially searches just the local facts.
 * If that search fails, then falls back to parent. (which in turn, could be
 * a CascadingFactBag).
 *
 * This approach differs from just merging facts together, as it allows facts
 * to exist in different contexts.
 *
 * For example, when projecting an Entity within a collection, that is an
 * attribute on a broader object.  We want to treat facts about the entity differently
 * from the global set of facts (whcih contain many entities)
 */
class CascadingFactBag(private val primary: FactBag, private val secondary: FactBag) : FactBag {
   constructor(primary: FactBag, secondary: TypedInstance, schema: Schema) : this(
      primary,
      CopyOnWriteFactBag(listOf(secondary), schema)
   )

   enum class CascadeApproach {
      /**
       * Join two collections returned, calling both primary and secondary
       */
      CombineCollections,

      /**
       * Only call the secondary if primary fails.
       */
      Cascade
   }

   /**
    * Merging a CascadingFactBag simply creates a new CascadingFactBag, with the
    * other as the primary.
    */
   override fun merge(other: FactBag): FactBag {
      return CascadingFactBag(other, this)
   }


   override fun merge(fact: TypedInstance): FactBag {
      return CascadingFactBag(primary.merge(fact), this.secondary)
   }

   override fun excluding(facts: Set<TypedInstance>): FactBag {
      return CascadingFactBag(this.primary.excluding(facts), this.secondary.excluding(facts))
   }

   override fun breadthFirstFilter(strategy: FactDiscoveryStrategy, predicate: (TypedInstance) -> Boolean): List<TypedInstance> {
      val fromPrimary = primary.breadthFirstFilter(strategy, predicate)
      val fromSecondary = if (strategy == FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) {
         secondary.breadthFirstFilter(strategy, predicate)
      } else {
         emptyList()
      }
      // This doesn't seem right -
      // Likely need to consider:
      // If fromPrimary OR fromSecondary are typedCollections, we'll need to flatten them out.
      return fromPrimary + fromSecondary
   }

   override fun addFact(fact: TypedInstance): FactBag {
      throw UnsupportedOperationException("CascadingFactBag does not support adds.")
   }

   override fun addFacts(facts: Collection<TypedInstance>): FactBag {
      throw UnsupportedOperationException("CascadingFactBag does not support adds.")
   }

   override fun hasFactOfType(type: Type, strategy: FactDiscoveryStrategy, spec: TypedInstanceValidPredicate): Boolean {
      return (primary.hasFactOfType(type, strategy, spec)) || secondary.hasFactOfType(type, strategy, spec)
   }

   private fun cascadingApproach(type: Type, strategy: FactDiscoveryStrategy): CascadeApproach {
      return if (strategy == FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY && type.isCollection) {
         CascadeApproach.CombineCollections
      } else {
         CascadeApproach.Cascade
      }
   }


   override fun getFact(type: Type, strategy: FactDiscoveryStrategy, spec: TypedInstanceValidPredicate): TypedInstance {
      return when (cascadingApproach(type, strategy)) {
         CascadeApproach.CombineCollections -> {
            combineCollections(
               primary.getFactOrNull(type, strategy, spec) as TypedCollection?,
               secondary.getFactOrNull(type, strategy, spec) as TypedCollection?,
               type,
               permitNull = false
            )!!
         }

         CascadeApproach.Cascade -> {
            return primary.getFactOrNull(
               type,
               strategy,
               spec
            ) // return null if no match in primary to allow fallthrough to parent
               ?: secondary.getFact(type, strategy, spec)
         }
      }
   }

   private fun combineCollections(
      a: TypedCollection?,
      b: TypedCollection?,
      type: Type,
      permitNull: Boolean
   ): TypedCollection? {
      if (a == null && b == null) {
         if (permitNull) {
            return null
         } else {
            return TypedCollection.empty(type)
         }
      }
      val populatedList = (a?.value ?: emptyList<TypedInstance>()) + (b?.value ?: emptyList<TypedInstance>())
      return TypedCollection.from(populatedList)
   }

   override fun getFactOrNull(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? {
      return when (cascadingApproach(type, strategy)) {
         CascadeApproach.CombineCollections -> {
            combineCollections(
               primary.getFactOrNull(type, strategy, spec) as TypedCollection?,
               secondary.getFactOrNull(type, strategy, spec) as TypedCollection?,
               type,
               permitNull = true
            )!!
         }

         CascadeApproach.Cascade -> {
            return primary.getFactOrNull(
               type,
               strategy,
               spec
            ) // return null if no match in primary to allow fallthrough to parent
               ?: secondary.getFact(type, strategy, spec)
         }
      }
   }

   override fun getFactOrNull(search: FactSearch): TypedInstance? {
      if (search.strategy == FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) {
         val primaryFact = primary.getFactOrNull(search)
         val secondaryFact = secondary.getFactOrNull(search)
         return combineIfPossible(primaryFact,secondaryFact)
      } else {
         return primary.getFactOrNull(search) ?: secondary.getFactOrNull(search)
      }
   }

   private fun combineIfPossible(primaryFact: TypedInstance?, secondaryFact: TypedInstance?): TypedInstance? {
      when {
         primaryFact == null && secondaryFact == null -> return null
         primaryFact != null && secondaryFact == null -> return primaryFact
         primaryFact == null && secondaryFact != null -> return secondaryFact
      }
      if (primaryFact is TypedCollection && secondaryFact is TypedCollection) {
         return TypedCollection.from(primaryFact + secondaryFact)
      }
      // We can't combine, so return just the primary, since that's our contract.
      // This may cause problems - if so, we should log and return null
      return primaryFact
   }

   override fun hasFact(search: FactSearch): Boolean {
      return primary.hasFact(search) || secondary.hasFact(search)
   }

   override val size: Int
      get() = primary.size + secondary.size

   override fun contains(element: TypedInstance): Boolean {
      return primary.contains(element) || secondary.contains(element)
   }

   override fun containsAll(elements: Collection<TypedInstance>): Boolean {
      return elements.all { contains(it) }
   }

   override fun isEmpty(): Boolean {
      return primary.isEmpty() && secondary.isEmpty()
   }

   override fun iterator(): Iterator<TypedInstance> {
      return Iterators.concat(primary.iterator(), secondary.iterator())
   }
}
