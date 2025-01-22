package com.orbitalhq.models.facts

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import lang.taxi.accessors.Argument
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.types.ArrayType
import lang.taxi.types.Arrays

data class ScopedFact(val scope: Argument, val fact: TypedInstance) {
   val type = fact.type
   val typeName = fact.typeName

   init {
      // If we hit this, it's a bug - it means a scope has been declared with one type,
      // but we're trying to construct it with a different type.
      // Root cause of ORB-877
//      if (!fact.type.isAssignableTo(scope.type.toVyneQualifiedName())) {
//         throw IllegalArgumentException("Provided fact of type ${fact.type.name.shortDisplayName} is not assignable to the declared scope of ${scope.type.toVyneQualifiedName().shortDisplayName}")
//      }
   }
}

/**
 * Experiment:
 * Returns a scope that represents an iteration of a collection from the provided scope.
 * Encapsulated as a method to provide consistent naming of the scope.
 *
 * This would allow reference to the source collection in scenarios such as:
 * find { Foo } as (things:Thing[]) -> {
 *    // ... here, things and things$member are both in scope
 * }
 *
 * If this turns out to make sense, promote it to ProjectionFunctionScope
 */
fun Argument.asIteratingScope(): ProjectionFunctionScope {
   require(Arrays.isArray(this.type)) { "Cannot call asIteratingScope on a type that is not an Array" }
   val memberType = (this.type as ArrayType).memberType
   return ProjectionFunctionScope(
      "${this.name}\$member", memberType

   )
}
