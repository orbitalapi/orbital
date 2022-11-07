package io.vyne.models.facts

import io.vyne.models.MixedSources
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.AlwaysGoodSpec
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.Type
import io.vyne.schemas.TypeMatchingPredicate
import io.vyne.schemas.TypeMatchingStrategy
import io.vyne.utils.ImmutableEquality
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class FactSearch(
   val name: String,
   val targetType: Type,
   val strategy: FactDiscoveryStrategy,
   /**
    * Predicate used to select TypedInstances from the facts.
    */
   val filterPredicate: (TypedInstance) -> Boolean,
   /**
    * In the event that many possible matches are found, strategies
    * may pass a predicate here to select the "best" value.
    * Alternatively, return null, which indicates that the search has failed.
    */
   val refiningPredicate: (List<TypedInstance>) -> TypedInstance? = NO_REFINING_PERMITTED,

   ) {
   private val equality = ImmutableEquality(
      this,
      FactSearch::name,
      FactSearch::strategy,
      FactSearch::filterPredicate,
      FactSearch::refiningPredicate
   )

   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }

   override fun hashCode(): Int {
      return equality.hash()
   }

   companion object {
      /**
       * Refining predicate that indicates no refining is allowed - ie.,
       * if we didn't get an exact match previously, don't try to refine the list further.
       */
      val NO_REFINING_PERMITTED: (List<TypedInstance>) -> TypedInstance? = { null }
      fun findType(
         type: Type,
         strategy: FactDiscoveryStrategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY,
         spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
         matcher: TypeMatchingPredicate = TypeMatchingStrategy.ALLOW_INHERITED_TYPES
      ): FactSearch {
         val predicate = { instance: TypedInstance ->
            matcher.matches(type, instance.type) && spec.isValid(instance)
         }

         val refiningPredicate: (List<TypedInstance>) -> TypedInstance? =
         // This has been migrated from the previous implementation.
         // We used to apply this logic ONLY on the ANY_DEPTH_EXPECT_ONE_DISTINCT
            // strategy, so applying it here for consistency.
            if (strategy == FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT) {
               { matches: List<TypedInstance> ->
                  val exactMatches = matches.filter { it.type == type }
                  if (exactMatches.size == 1) {
                     exactMatches.first()
                  } else {
                     null
                  }
               }
            } else {
               NO_REFINING_PERMITTED
            }
         return FactSearch("Find type ${type.name.shortDisplayName}", type, strategy, predicate, refiningPredicate)
      }
   }
}

enum class FactDiscoveryStrategy {
   TOP_LEVEL_ONLY {
      override fun getFact(
         facts: FactBag,
         search: FactSearch
      ): TypedInstance? {
         return facts.firstOrNull { search.filterPredicate(it) }
      }
   },

   /**
    * Will return a match from any depth, providing there is
    * exactly one match in the context
    */
   ANY_DEPTH_EXPECT_ONE {
      override fun getFact(
         facts: FactBag,
         search: FactSearch
      ): TypedInstance? {
         val matches = facts
            .breadthFirstFilter(ANY_DEPTH_EXPECT_ONE) { search.filterPredicate(it) }
            .toList()
         return when {
            matches.isEmpty() -> null
            matches.size == 1 -> matches.first()
            else -> {
               logger.debug {
                  "ANY_DEPTH_EXPECT_ONE strategy found ${matches.size} for search ${search.name}, so returning null"
               }
               null
            }

         }
      }
   },

   /**
    * Will return matches from any depth, providing there is exactly
    * one DISTINCT match within the context
    */
   ANY_DEPTH_EXPECT_ONE_DISTINCT {
      override fun getFact(
         facts: FactBag,
         search: FactSearch
      ): TypedInstance? {
         val matches = facts
            .breadthFirstFilter(ANY_DEPTH_EXPECT_ONE) { search.filterPredicate(it) }
            .distinct()
            .toList()
         return when {
            matches.isEmpty() -> null
            matches.size == 1 -> toCollectionIfRequested(matches.first(), search.targetType)
            else -> {
               // last ditch attempt
               val refinedSelection = search.refiningPredicate(matches)
               if (refinedSelection != null) {
                  refinedSelection
               } else {
                  val nonNullMatches = matches.filter { it.value != null }
                  if (nonNullMatches.size == 1) {
                     toCollectionIfRequested(nonNullMatches.first(), search.targetType)
                  } else {
                     logger.debug {
                        "ANY_DEPTH_EXPECT_ONE strategy found ${matches.size} of search ${search.name}, so returning null"
                     }
                     null
                  }
               }
            }
         }
      }
   },

   /**
    * Will return matches from any depth/
    * Returns a collection of results
    */
   ANY_DEPTH_ALLOW_MANY {
      override fun getFact(
         factBag: FactBag,
         search: FactSearch
      ): TypedCollection? {
         val matches = factBag
            .breadthFirstFilter(ANY_DEPTH_ALLOW_MANY) { search.filterPredicate(it) }
            .distinct()
            .toList()
         return when {
            matches.isEmpty() -> null
            else -> TypedCollection.flatten(matches, MixedSources.singleSourceOrMixedSources(matches))
         }
      }
   };



   abstract fun getFact(
      facts: FactBag,
      search: FactSearch
   ): TypedInstance?


}

/**
 * If the requested type is a collection, then returns the instance
 * wrapped in a collection.
 *
 * Otherwise, returns the instance as-is.
 */
private fun toCollectionIfRequested(singleInstance: TypedInstance, targetType: Type): TypedInstance {
   return if (targetType.isCollection) {
      if (singleInstance is TypedCollection) {
         return singleInstance
      } else {
         return TypedCollection.from(listOf(singleInstance))
      }
   } else {
      singleInstance
   }
}
