package com.orbitalhq.query.projection

import com.orbitalhq.models.PermittedQueryStrategies
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.ValueLookupReturnedNull
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.query.QueryContext
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.types.ArrayType
import lang.taxi.types.StreamType

/**
 * This class is responsible for taking
 * the inputs to a projection (ProjectionFunctionScope)
 * and providing the actual values as ScopedFacts.
 *
 * Eg:
 *
 * find { Foo } as ( thing : Thing ) -> {
 * }
 *
 * Often, when projecting, we're operating against
 * a primary input value.
 * (For things like Policies, we don't have a main input, so we use the facts in the current
 * query context).
 *
 * The primary inputs are used for resolving against,
 * if provided.
 *
 * If resolution against the primary input fails,
 * we fallback to a context query.
 *
 */
object ProjectionFunctionScopeEvaluator {
   fun build(
      inputs: List<ProjectionFunctionScope>,
      primaryFacts: List<TypedInstance>,//should be primaryFacts : List<TypedInstance> (I think)
      context: QueryContext,
   ):List<ScopedFact> {
      return inputs.map { scope ->
         val isAssignable = isAssignable(scope, primaryFacts)

         val schema = context.schema
         if (!isAssignable) {
            val scopeType = schema.type(scope.type)
            val selectedFact = try {
               // If the scope has an expression, evaluate it
               if (scope.expression != null) {
                  context.only(primaryFacts, context.scopedFacts)
                     .evaluate(scope.expression!!)
               } else {
                  // Otherwise, try and get the fact.
                  // First, search the fact bag
                  val fact = FactBag.of(primaryFacts, schema)
                     .getFactOrNull(scopeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
                  // If that didn't work, do a proper search
                  fact ?: runBlocking {
                     queryContextForFact(context, primaryFacts, scopeType)
                  }
               }

            } catch (e: Exception) {
               TypedNull.create(
                  scopeType, source = ValueLookupReturnedNull(
                     "Projection scope requested type ${scopeType.qualifiedName.shortDisplayName}, which was not found on the provided values of ${primaryFacts.joinToString { it.typeName }}",
                     scopeType.name
                  )
               )
            }
            ScopedFact(scope, selectedFact)
         } else {
            ScopedFact(scope, primaryFacts.single())
         }

      }
   }

   private suspend fun queryContextForFact(
      context: QueryContext,
      facts: List<TypedInstance>,
      scopeType: Type
   ): TypedInstance {
      val fromSearch = context.only(facts, context.scopedFacts)
         // Don't do a model scan, since we've already done one in the fact bag search
         .find(scopeType.paramaterizedName, permittedStrategy = PermittedQueryStrategies.EXCLUDE_BUILDER_AND_MODEL_SCAN)
         .results
         .toList()

      TODO()
   }

   /**
    * Indicates if the primary facts are a single value,
    * and that value is assignable to the scope.
    *
    * The choice around "primary value" here is legacy, and may change.
    * It's because this is called when projecting a value, where there's a clear
    * single input.
    * However, if there are multiple inputs, it MAY make sense to select the
    * first assignable value - if present.
    * However, that use-case hasn't presented yet.
    */
   private fun isAssignable(
      scope: ProjectionFunctionScope,
      primaryFacts: List<TypedInstance>
   ): Boolean {
      if (primaryFacts.size != 1) {
         return false
      }
      val primaryValue = primaryFacts.single()
      val emittedType = primaryValue.type
      val isAssignable = when (scope.type) {
         is ArrayType -> emittedType.taxiType.isAssignableTo((scope.type as ArrayType).memberType)
         is StreamType -> emittedType.taxiType.isAssignableTo((scope.type as StreamType).type)
         else -> emittedType.taxiType.isAssignableTo(scope.type)
      }
      return isAssignable
   }
}
