package io.vyne.queryService.query

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.models.OperationResult
import io.vyne.queryService.history.QueryCompletedEvent
import io.vyne.queryService.history.QueryEvent
import io.vyne.queryService.history.QueryEventConsumer
import io.vyne.queryService.history.QueryFailureEvent
import io.vyne.queryService.history.RestfulQueryExceptionEvent
import io.vyne.queryService.history.RestfulQueryResultEvent
import io.vyne.queryService.history.TaxiQlQueryExceptionEvent
import io.vyne.queryService.history.TaxiQlQueryResultEvent
import org.springframework.stereotype.Component


@Component
class MetricsEventConsumer(val meterRegistry: MeterRegistry) : QueryEventConsumer {

    init {
        counterQueryResults = Counter
            .builder("vyne.query.results")
            .baseUnit("result")
            .description("Count of results records emitted")
            .register(meterRegistry)

        counterQueryException = Counter
            .builder("vyne.query.exception")
            .baseUnit("result")
            .description("Count of results records emitted")
            .register(meterRegistry)

        counterSuccessfulQueries = Counter
            .builder("vyne.query.success")
            .baseUnit("query")
            .description("Count of queries")
            .register(meterRegistry)

        counterFailedQueries = Counter
            .builder("vyne.query.failed")
            .baseUnit("query")
            .description("Count of queries")
            .register(meterRegistry)

        counterGraphSearch = Counter
            .builder("vyne.query.graph.success")
            .baseUnit("search")
            .description("Count graph searches")
            .register(meterRegistry)

        counterGraphFailedSearch = Counter
            .builder("vyne.query.graph.failed")
            .baseUnit("search")
            .description("Count failed graph searches")
            .register(meterRegistry)
    }

    companion object {
        var counterQueryResults: Counter? = null
        var counterQueryException: Counter? = null
        var counterSuccessfulQueries: Counter? = null
        var counterFailedQueries: Counter? = null
        var counterGraphSearch: Counter? = null
        var counterGraphFailedSearch: Counter? = null

    }

    override fun handleEvent(event: QueryEvent) {

        when (event) {
            is TaxiQlQueryResultEvent -> {counterQueryResults?.increment()}
            is RestfulQueryResultEvent -> {counterQueryResults?.increment()}
            is QueryCompletedEvent -> {counterSuccessfulQueries?.increment()}
            is QueryFailureEvent -> {counterFailedQueries?.increment()}
            is RestfulQueryExceptionEvent -> {counterQueryException?.increment()}
            is TaxiQlQueryExceptionEvent -> {counterQueryException?.increment()}
        }
    }

    override fun recordResult(operation: OperationResult, queryId: String) {
        println("MetricsEventConsumer recordResult")
    }

}