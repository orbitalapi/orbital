package io.vyne.models

import io.vyne.schemas.Type
import io.vyne.utils.log

/**
 * When a request for a specific typed instance has been made,
 * and multiple candidates are found, try to resolve the best match
 * where possible.
 *
 */
object TypedInstanceCandidateFilter {
   fun resolve(candidates: Collection<TypedInstance>, requestedType: Type): TypedInstance {
      // bail early if we can
      if (candidates.size == 1) {
         return candidates.first()
      }
      // eliminate nulls:
      val nonNulls = candidates.filter {
         when (val value = it.value) {
            null -> false
            is String -> value.isNotEmpty()
            else -> true
         }
      }
      if (nonNulls.size == 1) {
         return nonNulls.first()
      }

      // find an exact match based on type if possible.
      // If there are no matches with exactly the same type, let's use the full set of
      // non-null candidates.  Otherwise, only consider the values with the exact match on type
      val bestTypeMatches = nonNulls.filter { it.type.name.parameterizedName == requestedType.name.parameterizedName }
         .let { exactMatches ->
            if (exactMatches.isNotEmpty()) {
               exactMatches
            } else {
               nonNulls
            }
         }

      if (bestTypeMatches.size == 1) {
         return bestTypeMatches.first()
      }

      // are all the values the same?
      if (bestTypeMatches.isNotEmpty() && bestTypeMatches.all { it.valueEquals(bestTypeMatches[0]) }) {
         // all the values are the same, so just return the first.
         return bestTypeMatches.first()
      }

      // Out of ideas, give up.
      val candidateDescription = bestTypeMatches.joinToString("\n") { "${it.type.name.parameterizedName} : ${it.value}" }
      log().info("returning TypedNull for $requestedType as candidates are $candidateDescription")
      return TypedNull(requestedType)
      //error("Ambiguous property - there are ${bestTypeMatches.size} possible matches for type ${requestedType.name.parameterizedName}, each with different values: $candidateDescription.  Consider restricting the requested type to a more specific type")
   }
}
