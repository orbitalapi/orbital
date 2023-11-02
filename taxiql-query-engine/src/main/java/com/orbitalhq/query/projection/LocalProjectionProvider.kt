package com.orbitalhq.query.projection

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.ValueLookupReturnedNull
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.query.Projection
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.TypeQueryExpression
import com.orbitalhq.query.VyneQueryStatistics
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lang.taxi.accessors.CollectionProjectionExpressionAccessor
import lang.taxi.types.ArrayType
import lang.taxi.types.StreamType
import mu.KotlinLogging
import java.util.concurrent.Executors

private val projectingDispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()
private val logger = KotlinLogging.logger {}

@OptIn(FlowPreview::class)
class LocalProjectionProvider : ProjectionProvider {

   private val projectingScope = CoroutineScope(projectingDispatcher)

   override fun project(
      results: Flow<TypedInstance>,
      projection: Projection,
      context: QueryContext,
      globalFacts: FactBag
   ): Flow<Pair<TypedInstance, VyneQueryStatistics>> {

      context.cancelFlux.subscribe {
         logger.info { "QueryEngine for queryId ${context.queryId} is cancelling" }
         projectingScope.cancel()
      }

      // This pattern aims to allow the concurrent execution of multiple flows.
      // Normally, flow execution is sequential - ie., one flow must complete befre the next
      // item is taken.  buffer() is used here to allow up to n parallel flows to execute.
      // MP: @Anthony - please leave some comments here that describe the rationale for
      // map { async { .. } }.flatMapMerge { await }
      return results
         .buffer()
         .withIndex()
         .takeWhile { !context.cancelRequested }
         .filter { !context.cancelRequested }
         .distinctUntilChanged()
         .map { emittedResult ->
            logger.debug { "Starting to project instance of ${emittedResult.value.type.qualifiedName.shortDisplayName} (index ${emittedResult.index}) to instance of ${projection.type.qualifiedName.shortDisplayName}" }

            projectingScope.async {
               if (!isActive) {
                  logger.warn { "Query Cancelled exiting!" }
                  cancel()
               }
               val projectionType = selectProjectionType(projection.type)
               val scopedFact = projection.scope?.let { scope ->
                  val emittedType = emittedResult.value.type
                  // Adding this guard clause.
                  // We're getting the incorrect type passed in.
                  // It's an upstream problbem, but if we let it flow any further, we spend huge CPU cycles
                  // trying to project the wrong source type.
                  // TODO : Investigate the cause - I suspect it's coming from the Graph search strategy, when service invocation fails.
                  val isAssignable = when (scope.type) {
                     is ArrayType -> emittedType.taxiType.isAssignableTo((scope.type as ArrayType).memberType)
                     is StreamType -> emittedType.taxiType.isAssignableTo((scope.type as StreamType).type)
                     else -> emittedType.taxiType.isAssignableTo(scope.type)
                  }

                  val schema = context.schema
                  if (!isAssignable) {
                     val scopeType = schema.type(scope.type)
                     val selectedFact = try {
                        FactBag.of(emittedResult.value, schema)
                           .getFact(scopeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
                     } catch (e:Exception) {
                        TypedNull.create(scopeType, source = ValueLookupReturnedNull(
                           "Projection scope requested type ${scopeType.qualifiedName.shortDisplayName}, which was not found on the type of ${emittedResult.value.typeName}",
                           scopeType.name
                        ))
                     }
                     ScopedFact(scope, selectedFact)
                  } else {
                     ScopedFact(scope, emittedResult.value)
                  }

               } ?: null

               // If the projection scope was explicitly defined,
               // add the thing we're projecting as a specific scoped fact.
               // This makes it available for both type-based-searches (standard),
               // and when searching by scope.
               // Otherwise, just add it as a normal fact at the root.
               // Note: In time, we should probably refactor so that there's ALWAYS a root
               // scope, with a name of "this" if not otherwise specified.
               val projectionContext = if (scopedFact == null) {
                  context.only(globalFacts.rootFacts() + emittedResult.value)
               } else {
                  context.only(globalFacts.rootFacts(), scopedFacts = listOf(scopedFact))
               }
               val buildResult = projectionContext.build(TypeQueryExpression(projectionType))
               buildResult.results.map { it to projectionContext.vyneQueryStatistics }
            }
         }
         .buffer(16).map { it.await() }.flatMapMerge { it }
   }

   private fun selectProjectionType(projectResultsTo: Type): Type {
      // MP: I don't understand why we're not attempting to project to the collection
      // type here, and instead pass the member type.  How do we end up constructing collections?
      // Unsure, but it works, and when I change this, oodles of tests break.
      // However, in circumstances where we need to construct a flattened collection,
      // we need to pass the collection type here, in order for the expression: "by [ThingToIterate with scope(..)]"
      // to get picked up.
      // This is clearly wrong - if it works for other collections, why can't it work here?
      // However, this is what I need to get this working now.
      return if (projectResultsTo.collectionType?.expression is CollectionProjectionExpressionAccessor) {
         projectResultsTo
      } else {
         // In all other cases, project to the collection member type, not the array.
         // No idea why.
         projectResultsTo.collectionType ?: projectResultsTo
      }
   }

}
