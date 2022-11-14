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

/**
 * A predicate, which has an additional id.
 * When we're caching predicates, we can't use the hashcode of the predicate
 * to reliably determine equality, so we use this id.
 */
interface PredicateWithId {
   /**
    * Used to determine equality of two strategies (instead of the predicate)
    */
   val id: String
}

interface FilterPredicateStrategy : PredicateWithId {
   /**
    * Predicate used to select TypedInstances from the facts.
    */
   val predicate: (TypedInstance) -> Boolean
}

interface RefiningPredicate : PredicateWithId {

   /**
    * In the event that many possible matches are found, strategies
    * may pass a predicate here to select the "best" value.
    * Alternatively, return null, which indicates that the search has failed.
    */
   val predicate: (List<TypedInstance>) -> TypedInstance?
}

data class FactSearch(
   val name: String,
   val targetType: Type,
   val strategy: FactDiscoveryStrategy,
   /**
    * Predicate used to select TypedInstances from the facts.
    */
   val filterPredicate: FilterPredicateStrategy,
   /**
    * In the event that many possible matches are found, strategies
    * may pass a predicate here to select the "best" value.
    * Alternatively, return null, which indicates that the search has failed.
    */
   val refiningPredicate: RefiningPredicate = NO_REFINING_PERMITTED,
) {
   private val targetTypeName = targetType.name.parameterizedName
   private val filterPredicateId = filterPredicate.id
   private val refiningPredicateId = refiningPredicate.id
   private val equality = ImmutableEquality(
      this,
      // Name is for display purposes only - not part of the hash, to ensure duplciate definitions get the sam hashcode
//      FactSearch::name,
      FactSearch::targetTypeName,
      FactSearch::strategy,
      FactSearch::filterPredicateId,
      FactSearch::refiningPredicateId
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
      val NO_REFINING_PERMITTED = object : RefiningPredicate {
         override val id: String = "NO_REFINING_PERMITTED"
         override val predicate: (List<TypedInstance>) -> TypedInstance? = { null }
      }

      fun refineToExactTypeMatch(type: Type): RefiningPredicate {
         return object : RefiningPredicate {
            override val id: String = "REFINE_TO_EXACT_TYPE_MATCH"
            override val predicate: (List<TypedInstance>) -> TypedInstance? = { matches: List<TypedInstance> ->
               val exactMatches = matches.filter { it.type == type }
               if (exactMatches.size == 1) {
                  exactMatches.first()
               } else {
                  null
               }
            }
         }
      }

      fun findType(
         type: Type,
         strategy: FactDiscoveryStrategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY,
         spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
         matcher: TypeMatchingPredicate = TypeMatchingStrategy.ALLOW_INHERITED_TYPES
      ): FactSearch {
         val predicate = object : FilterPredicateStrategy {
            override val id = matcher.id
            override val predicate: (TypedInstance) -> Boolean = { instance: TypedInstance ->
               matcher.matches(type, instance.type) && spec.isValid(instance)
            }
         }

         val refiningPredicate: RefiningPredicate =
         // This has been migrated from the previous implementation.
         // We used to apply this logic ONLY on the ANY_DEPTH_EXPECT_ONE_DISTINCT
            // strategy, so applying it here for consistency.
            if (strategy == FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT) {
               refineToExactTypeMatch(type)
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
         return facts.firstOrNull { search.filterPredicate.predicate(it) }
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
            .breadthFirstFilter(ANY_DEPTH_EXPECT_ONE, FactMapTraversalStrategy.enterIfHasFieldOfType(search.targetType)) { search.filterPredicate.predicate(it) }
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
            .breadthFirstFilter(ANY_DEPTH_EXPECT_ONE, FactMapTraversalStrategy.enterIfHasFieldOfType(search.targetType)) { search.filterPredicate.predicate(it) }
            .distinct()
            .toList()
         return when {
            matches.isEmpty() -> null
            matches.size == 1 -> toCollectionIfRequested(matches.first(), search.targetType)
            else -> {
               // last ditch attempt
               val refinedSelection = search.refiningPredicate.predicate(matches)
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
            .breadthFirstFilter(ANY_DEPTH_ALLOW_MANY, FactMapTraversalStrategy.enterIfHasFieldOfType(search.targetType)) { search.filterPredicate.predicate(it) }
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
