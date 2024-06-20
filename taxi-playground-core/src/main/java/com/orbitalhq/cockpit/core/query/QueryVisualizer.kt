package com.orbitalhq.cockpit.core.query

import com.orbitalhq.history.chart.LineageSankeyViewBuilder
import com.orbitalhq.models.OperationResult
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.TaxiQlQueryResultEvent
import com.orbitalhq.query.history.QuerySankeyChartRow
import com.orbitalhq.schemas.Schema
import com.orbitalhq.stubbing.StubService
import kotlinx.coroutines.runBlocking
import lang.taxi.query.TaxiQLQueryString

/**
 * This class generates a query plan (for visualisation,
 * not query optimisation / planning).
 *
 * The intent is to show users a visualization of a query
 * before running it.
 *
 */
class QueryVisualizer {
   fun visualizeQuery(query: TaxiQLQueryString, schema: Schema): List<QuerySankeyChartRow> {
      // This belongs in the service
      val (vyne, stubService) = StubService.stubbedVyne(schema)
      stubService.returnStubValuesForAllOperations()
      val lineageEventBroker = QueryContextEventBroker()
      val viewBuilder = LineageSankeyViewBuilder(schema)
      lineageEventBroker.addHandler(QueryPlanEventHandler(viewBuilder))


      runBlocking {
         vyne.query(
            query,
            eventBroker = lineageEventBroker
         )
            .results.collect { instance ->
               viewBuilder.append(instance)
            }
      }
      return viewBuilder.asChartRows("")
   }
}

private class QueryPlanEventHandler(private val sankeyViewBuilder: LineageSankeyViewBuilder) : QueryEventConsumer {
   override fun handleEvent(event: QueryEvent) {
      if (event is TaxiQlQueryResultEvent) {
         sankeyViewBuilder.append(event.typedInstance)
      }
   }

   override fun recordResult(operation: OperationResult, queryId: String) {
      sankeyViewBuilder.captureOperationResult(operation)
   }


}
