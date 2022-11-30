package io.vyne.models.facts

import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Type
import io.vyne.utils.ImmutableEquality
import io.vyne.utils.xtimed
import lang.taxi.types.ArrayType
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import java.util.concurrent.TimeUnit


sealed class TreeNavigationInstruction {
   fun combine(other: TreeNavigationInstruction): TreeNavigationInstruction {
      if (this is FullScan || other is FullScan) {
         return FullScan
      }
      return when {
         this is IgnoreThisElement && other is IgnoreThisElement -> IgnoreThisElement
         this is IgnoreThisElement && other is EvaluateSpecificFields -> other
         this is EvaluateSpecificFields && other is IgnoreThisElement -> this
         this is EvaluateSpecificFields && other is EvaluateSpecificFields -> this.plus(other)
         else -> error("Unhandled TreeNavigation comparison : ${this::class.simpleName} vs ${other::class.simpleName}")
      }
   }
}


object IgnoreThisElement : TreeNavigationInstruction()
data class EvaluateSpecificFields(val fieldNames: Set<String>) : TreeNavigationInstruction() {
   fun plus(other:EvaluateSpecificFields) = EvaluateSpecificFields(this.fieldNames + other.fieldNames)
   fun filter(instance: TypedObject): List<TypedInstance> {
      val result = fieldNames.flatMap { instance.getAllAtPath(it) }
      return result
   }
}

object FullScan : TreeNavigationInstruction()

/**
 * Strategies that determine if a search in a fact bag should
 * interrogate an object any further
 */
data class FactMapTraversalStrategy(val name: String, val predicate: (TypedInstance) -> TreeNavigationInstruction) {
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

         val predicate: (TypedInstance) -> TreeNavigationInstruction = { instance ->
            xtimed("enterIfHasFieldOfType ${searchType.longDisplayName}", timeUnit = TimeUnit.NANOSECONDS) {
               if (searchTaxiType == PrimitiveType.ANY) {
                  // If someone asks for Any, we go the full depth
                  FullScan
               } else if (searchType.isEnum) {
                  // TODO : This needs to be optimized.
                  // We can't use simple "path to value" semantics here,
                  // as we need to support synonym matching.
                  // So, for now we use a FullScan.
                  // However, this can be improved by:
                  // * Checking the schema to see which enums have valid synonyms for the requested type (considering transitive synonyms)
                  // * Scanning for each of the possible enum types.
                  // Will consider this in a future release
                  FullScan
               } else if (searchType.isCollection) {
                  // Our search implementation can treat searching for an array in two different ways:
                  // - Find all things of T, and collect into an array
                  // - Find all collections of T
                  // So, we search for both
                  enterIfHasFieldOfType(
                     searchTaxiType,
                     instance.type.taxiType
                  ).combine(enterIfHasFieldOfType(searchType.collectionType!!.taxiType, instance.type.taxiType))
               } else {
                  enterIfHasFieldOfType(searchTaxiType, instance.type.taxiType)
               }
            }
         }
         return FactMapTraversalStrategy("Enter if has field of type ${searchType.longDisplayName}", predicate)
      }

      private fun enterIfHasFieldOfType(
         searchType: lang.taxi.types.Type,
         instanceType: lang.taxi.types.Type
      ): TreeNavigationInstruction {
         return when {
//            instanceType.isAssignableTo(searchType) -> true
            instanceType is ObjectType -> {
               val paths = instanceType.getDescendantPathsOfType(searchType).toSet()
               if (paths.isEmpty()) {
                  IgnoreThisElement
               } else {
                  EvaluateSpecificFields(paths)
               }

//               instanceType.hasDescendantWithType(searchType)
            }

            instanceType is ArrayType -> {
               val memberType = instanceType.memberType
               if (memberType == PrimitiveType.ANY) {
                  // A collection of Any must be traversed
                  FullScan
               } else {
                  enterIfHasFieldOfType(searchType, instanceType.memberType)
               }

            } /* && taxiType.memberType is ObjectType -> {
               val memberType = taxiType.memberType as ObjectType
               memberType.isAssignableTo(searchTaxiType) || memberType.hasDescendantWithType(searchTaxiType)
            } */
            else -> IgnoreThisElement
         }
      }
   }

}
