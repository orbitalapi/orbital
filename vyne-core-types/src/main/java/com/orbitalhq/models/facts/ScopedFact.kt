package com.orbitalhq.models.facts

import com.orbitalhq.models.TypedInstance
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.types.ArrayType
import lang.taxi.types.Arrays

data class ScopedFact(val scope: ProjectionFunctionScope, val fact: TypedInstance) {
   val type = fact.type
   val typeName = fact.typeName
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
fun ProjectionFunctionScope.asIteratingScope(): ProjectionFunctionScope {
   require(Arrays.isArray(this.type)) { "Cannot call asIteratingScope on a type that is not an Array" }
   val memberType = (this.type as ArrayType).memberType
   return ProjectionFunctionScope(
      "${this.name}\$member", memberType

   )
}
