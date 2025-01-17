package com.orbitalhq.cockpit.core.query

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.history.chart.LineageSankeyViewBuilder
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.connectors.OperationInvocationPlanner
import com.orbitalhq.query.history.QuerySankeyChartRow
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
   fun visualizeQuery(query: TaxiQLQueryString, schema: Schema): Either<Exception, List<QuerySankeyChartRow>> {
      // This belongs in the service
      val (vyne, stubService) = StubService.stubbedVyne(schema, planners, stateStoreProvider)
      stubService.returnStubValuesForAllOperations()
      val lineageEventBroker = QueryContextEventBroker()
      val viewBuilder = LineageSankeyViewBuilder(schema)
      lineageEventBroker.addHandler(QueryPlanEventHandler(viewBuilder))


      val either = runBlocking {
         try {
            vyne.query(
               query,
               eventBroker = lineageEventBroker
            )
               .results.collect { instance ->
                  viewBuilder.append(instance)
               }
            Unit.right()
         } catch (e:Exception) {
            logger.info { "Failed to generate query visualisation:  ${e::class.simpleName} - ${e.message}" }
            e.left()
         }
      }
      return either.map {
         viewBuilder.asChartRows("")
      }
   }
}

class QueryPlanEventHandler(val sankeyViewBuilder: LineageSankeyViewBuilder) : QueryEventConsumer {
   companion object {
      fun createFor(schema:Schema):QueryPlanEventHandler {
         val builder = LineageSankeyViewBuilder(schema)
         return QueryPlanEventHandler(builder)
      }
   }

   fun appendResult(instance: TypedInstance) {
      sankeyViewBuilder.append(instance)
   }
   override fun handleEvent(event: QueryEvent) {
   }

   override fun recordResult(operation: OperationResult, queryId: String) {
      sankeyViewBuilder.captureOperationResult(operation)
   }


}
