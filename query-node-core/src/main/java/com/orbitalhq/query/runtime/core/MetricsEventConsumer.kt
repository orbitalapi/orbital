package com.orbitalhq.query.runtime.core

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import com.orbitalhq.models.OperationResult
import com.orbitalhq.query.QueryCompletedEvent
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.QueryFailureEvent
import com.orbitalhq.query.RestfulQueryExceptionEvent
import com.orbitalhq.query.RestfulQueryResultEvent
import com.orbitalhq.query.TaxiQlQueryExceptionEvent
import com.orbitalhq.query.TaxiQlQueryResultEvent
import com.orbitalhq.query.VyneQueryStatistics
import com.orbitalhq.query.VyneQueryStatisticsEvent
import org.springframework.stereotype.Component


@Component
class MetricsEventConsumer(val meterRegistry: MeterRegistry) : QueryEventConsumer {

    val counterQueryResults = Counter
        .builder("vyne.query.results")
        .baseUnit("result")
        .description("Count of results records emitted")
        .register(meterRegistry)

    val counterQueryException = Counter
        .builder("vyne.query.exception")
        .baseUnit("result")
        .description("Count of results records emitted")
        .register(meterRegistry)

    val counterSuccessfulQueries = Counter
        .builder("vyne.query.success")
        .baseUnit("query")
        .description("Count of queries")
        .register(meterRegistry)

    val counterFailedQueries = Counter
        .builder("vyne.query.failed")
        .baseUnit("query")
        .description("Count of queries")
        .register(meterRegistry)

    val counterGraphSearch = Counter
        .builder("vyne.query.graph.search.success")
        .baseUnit("search")
        .description("Count graph searches")
        .register(meterRegistry)

    val counterGraphFailedSearch = Counter
        .builder("vyne.query.graph.search.failed")
        .baseUnit("search")
        .description("Count failed graph searches")
        .register(meterRegistry)

    val counterGraphBuild = Counter
        .builder("vyne.query.graph.search.build")
        .baseUnit("graph")
        .description("Count graphs built")
        .register(meterRegistry)

    override fun handleEvent(event: QueryEvent) {

        when (event) {
            is TaxiQlQueryResultEvent -> counterQueryResults.increment()
            is RestfulQueryResultEvent ->  counterQueryResults.increment()
            is QueryCompletedEvent -> counterSuccessfulQueries.increment()
            is QueryFailureEvent -> counterFailedQueries.increment()
            is RestfulQueryExceptionEvent -> counterQueryException.increment()
            is TaxiQlQueryExceptionEvent -> counterQueryException.increment()
            is VyneQueryStatisticsEvent -> {
               counterGraphFailedSearch.increment(event.vyneQueryStatistics.graphSearchFailedCount.toDouble())
               counterGraphSearch.increment(event.vyneQueryStatistics.graphSearchSuccessCount.toDouble())
               counterGraphBuild.increment(event.vyneQueryStatistics.graphCreatedCount.toDouble())
            }
           else -> {}
        }
    }

    override fun recordResult(operation: OperationResult, queryId: String) {

    }

}
