package io.vyne.query.projection

import io.vyne.models.TypedInstance
import io.vyne.models.facts.FactBag
import io.vyne.models.facts.ScopedFact
import io.vyne.query.Projection
import io.vyne.query.QueryContext
import io.vyne.query.QueryResult
import io.vyne.query.VyneQueryStatistics
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.toVyneQualifiedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
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
               val (scopedFact, isProjectable) = projection.scope?.let { scope ->
                  val emittedType = emittedResult.value.type
                  // Adding this guard clause.
                  // We're getting the incorrect type passed in.
                  // It's an upstream problbem, but if we let it flow any further, we spend huge CPU cycles
                  // trying to project the wrong source type.
                  // TODO : Investigate the cause - I suspect it's coming from the Graph search strategy, when service invocation fails.
                  val isProjectable = when (scope.type) {
                     is ArrayType -> (scope.type as ArrayType).memberType.isAssignableTo(emittedType.taxiType)
                     is StreamType -> (scope.type as StreamType).type.isAssignableTo(emittedType.taxiType)
                     else -> scope.type.isAssignableTo(emittedType.taxiType)
                  }
                  if (!isProjectable) {
                     val message =
                        "Projecting has received an invalid type - expected to receive an instance of ${scope.type.toVyneQualifiedName().shortDisplayName} but received an instance of ${emittedResult.value.type.qualifiedName.shortDisplayName}.  This is an error upstream, but aborting projection now"
                     logger.error { message }
                  }
                  ScopedFact(scope, emittedResult.value) to isProjectable
               } ?: (null to true)

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
               val buildResult = projectionContext.build(projectionType.qualifiedName)
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
