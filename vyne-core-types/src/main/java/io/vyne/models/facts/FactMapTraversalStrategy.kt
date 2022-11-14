package io.vyne.models.facts

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.schemas.Type
import io.vyne.utils.ImmutableEquality
import io.vyne.utils.timed
import io.vyne.utils.xtimed
import lang.taxi.types.ArrayType
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import java.util.concurrent.TimeUnit

/**
 * Strategies that determine if a search in a fact bag should
 * interrogate an object any further
 */
data class FactMapTraversalStrategy(val name: String, val predicate: (TypedInstance) -> Boolean) {
   private val equality = ImmutableEquality(
      this,
      FactMapTraversalStrategy::name
   )

   override fun hashCode(): Int {
      return equality.hash()
   }

   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }



   companion object {

      /**
       * Only traverses into an object is the type of the object could possibly
       * have any children of the desired search type
       */
      fun enterIfHasFieldOfType(searchType: Type): FactMapTraversalStrategy {
         val searchTaxiType = searchType.taxiType

         val predicate : (TypedInstance) -> Boolean = { instance ->
            xtimed("enterIfHasFieldOfType ${searchType.longDisplayName}", timeUnit = TimeUnit.NANOSECONDS) {
               if (searchType.isCollection) {
                  // Our search implementation can treat searching for an array in two different ways:
                  // - Find all things of T, and collect into an array
                  // - Find all collections of T
                  // So, we search for both
                  enterIfHasFieldOfType(
                     searchTaxiType,
                     instance.type.taxiType
                  ) || enterIfHasFieldOfType((searchTaxiType as ArrayType).memberType, instance.type.taxiType)
               } else {
                  enterIfHasFieldOfType(searchTaxiType, instance.type.taxiType)
               }
            }
         }
         return FactMapTraversalStrategy("Enter if has field of type ${searchType.longDisplayName}",predicate)
      }

      private fun enterIfHasFieldOfType(searchType: lang.taxi.types.Type, instanceType: lang.taxi.types.Type): Boolean {
         return when {
//            instanceType.isAssignableTo(searchType) -> true
            instanceType is ObjectType -> instanceType.hasDescendantWithType(searchType)
            instanceType is ArrayType -> {
               val memberType = instanceType.memberType
               if (memberType == PrimitiveType.ANY) {
                  // A collection of Any must be traversed
                  true
               } else {
                  enterIfHasFieldOfType(searchType, instanceType.memberType)
               }

            } /* && taxiType.memberType is ObjectType -> {
               val memberType = taxiType.memberType as ObjectType
               memberType.isAssignableTo(searchTaxiType) || memberType.hasDescendantWithType(searchTaxiType)
            } */
            else -> false
         }
      }
   }

}
