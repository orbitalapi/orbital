package io.vyne.query

import io.vyne.schemas.Schema

interface HistoryEventConsumerProvider {
   fun createEventConsumer(queryId: String, schema:Schema): QueryEventConsumer
}
