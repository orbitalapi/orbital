package com.orbitalhq.models

import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.runBlocking
import lang.taxi.accessors.Argument
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
      inputs: List<Argument>,
      primaryFacts: List<TypedInstance>,//should be primaryFacts : List<TypedInstance> (I think)
      context: InPlaceQueryEngine,
   ):List<ScopedFact> {
      return inputs.map { scope ->
         val isAssignable = isAssignable(scope, primaryFacts)

         val schema = context.schema
         if (!isAssignable) {
            val scopeType = schema.type(scope.type)
            val selectedFact = try {
               // If the scope has an expression, evaluate it
               if (scope is ProjectionFunctionScope && scope.expression != null) {
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
      context: InPlaceQueryEngine,
      facts: List<TypedInstance>,
      scopeType: Type
   ): TypedInstance {

      // 24-Jun-24: The below code existed (along with the TODO),
      // which suggested that this code was never called.
      // I need to downgrade the QueryEngine reference to InPlaceQueryEngine
      // to allow this to be callable from core-types
      // rather than query-engine.

      // It seems that this code should be reachable
      // (eg: if an input into a function can't come from the fact bag, and we
      // need to query for it).
      // When we come to implement this, it should be simple, as
      // there's an existing method on QueryContext we need to make implement the interface
      // All that's outstanding is handling the many-results from results to a single result defined in
      // the signature.
//      val fromSearch = context.only(facts, context.scopedFacts)
//         // Don't do a model scan, since we've already done one in the fact bag search
//         .find(scopeType.paramaterizedName, permittedStrategy = PermittedQueryStrategies.EXCLUDE_BUILDER_AND_MODEL_SCAN)
//         .results
//         .toList()

      TODO("querying a context for a fact as a scoped function isn't yet implemented")
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
      scope: Argument,
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
