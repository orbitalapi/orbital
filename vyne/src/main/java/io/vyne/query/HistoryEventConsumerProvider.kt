package io.vyne.query

interface HistoryEventConsumerProvider {
   fun createEventConsumer(queryId: String): QueryEventConsumer
}
