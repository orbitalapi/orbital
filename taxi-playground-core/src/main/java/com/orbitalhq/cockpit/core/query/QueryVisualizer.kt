package com.orbitalhq.cockpit.core.query

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.history.chart.LineageSankeyViewBuilder
import com.orbitalhq.history.chart.QueryVisualizationBuilder
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.connectors.OperationInvocationPlanner
import com.orbitalhq.query.history.QueryPlanDiagramData
import com.orbitalhq.query.history.QuerySankeyChartRow
import com.orbitalhq.query.tracing.TraceContext
import com.orbitalhq.schemas.Schema
import com.orbitalhq.stubbing.StubService
import kotlinx.coroutines.runBlocking
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging

/**
 * This class generates a query plan (for visualisation,
 * not query optimisation / planning).
 *
 * The intent is to show users a visualization of a query
 * before running it.
 *
 */
class QueryVisualizer(
   private val planners: List<OperationInvocationPlanner> = emptyList(),
   private val stateStoreProvider: StateStoreProvider? = null
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   // New approach - generates QueryPlanDiagramData
   fun generateQueryDiagram(query: TaxiQLQueryString, schema: Schema, arguments: Map<String,Any?> = emptyMap()): Either<Exception, QueryPlanDiagramData> {
      val lineageEventBroker = QueryContextEventBroker(traceSpan = TraceContext.noOp().rootSpan)
      val viewBuilder = QueryPlanDiagramBuilder(schema)
      lineageEventBroker.addHandler(QueryPlanEventHandler(viewBuilder))

      return runQueryAndGenerateDiagramData(schema, query, viewBuilder, arguments)
   }

   // Generates legacy QuerySankeyChartRow query data
   fun visualizeQuery(query: TaxiQLQueryString, schema: Schema, arguments: Map<String, Any?> = emptyMap()): Either<Exception, List<QuerySankeyChartRow>> {
      val viewBuilder = LineageSankeyViewBuilder(schema)
      return runQueryAndGenerateDiagramData(schema, query, viewBuilder, arguments = arguments)
   }

   private fun <T> runQueryAndGenerateDiagramData(
      schema: Schema,
      query: TaxiQLQueryString,
      viewBuilder: QueryVisualizationBuilder<T>,
      arguments: Map<String, Any?>
   ): Either<Exception, T> {
      val (vyne, stubService) = StubService.stubbedVyne(schema, planners, stateStoreProvider)
      stubService.returnStubValuesForAllOperations()

      val lineageEventBroker = QueryContextEventBroker(traceSpan = TraceContext.noOp().rootSpan)
      lineageEventBroker.addHandler(QueryPlanEventHandler(viewBuilder))

      val either = runBlocking {
         try {
            vyne.query(
               query,
               arguments = arguments,
               eventBroker = lineageEventBroker
            )
               .results.collect { instance ->
                  viewBuilder.append(instance)
               }
            Unit.right()
         } catch (e: Exception) {
            logger.info { "Failed to generate query visualisation:  ${e::class.simpleName} - ${e.message}" }
            e.left()
         }
      }
      return either.map {
         viewBuilder.build("")
      }
   }
}

class QueryPlanEventHandler(val diagramBuilder: QueryVisualizationBuilder<*>) : QueryEventConsumer {
   companion object {
      fun createFor(schema: Schema): QueryPlanEventHandler {
         val builder = LineageSankeyViewBuilder(schema)
         return QueryPlanEventHandler(builder)
      }
   }

   fun appendResult(instance: TypedInstance) {
      diagramBuilder.append(instance)
   }

   override fun handleEvent(event: QueryEvent) {
      println("Ignored method")
   }

   override fun recordResult(operation: OperationResult, queryId: String) {
      diagramBuilder.captureOperationResult(operation)
   }

   override fun reportCachedOperationWithUniquePathObserved(operation: OperationResultReference, queryId: String) {
      diagramBuilder.captureCachedOperationWithUniquePathObserved(operation, queryId)
   }


}
