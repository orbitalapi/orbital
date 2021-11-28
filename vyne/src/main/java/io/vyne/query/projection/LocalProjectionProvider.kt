package io.vyne.query.projection

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.VyneQueryStatistics
import io.vyne.schemas.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import lang.taxi.accessors.CollectionProjectionExpressionAccessor
import java.util.concurrent.Executors

private val projectingDispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()

class LocalProjectionProvider : ProjectionProvider {

   private val projectingScope = CoroutineScope(projectingDispatcher)

   override fun project(
      results: Flow<TypedInstance>,
      context: QueryContext
   ): Flow<Pair<TypedInstance, VyneQueryStatistics>> {

      // This pattern aims to allow the concurrent execution of multiple flows.
      // Normally, flow execution is sequential - ie., one flow must complete befre the next
      // item is taken.  buffer() is used here to allow up to n parallel flows to execute.
      // MP: @Anthony - please leave some comments here that describe the rationale for
      // map { async { .. } }.flatMapMerge { await }
      return results.buffer().withIndex()
         .filter { !context.cancelRequested }.map {
            projectingScope.async {
               val projectionType = selectProjectionType(context.projectResultsTo!!)
               val projectionContext = context.only(it.value)
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
