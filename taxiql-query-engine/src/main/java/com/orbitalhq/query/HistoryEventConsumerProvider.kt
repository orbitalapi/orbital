package com.orbitalhq.query

import com.orbitalhq.schemas.Schema

interface HistoryEventConsumerProvider {
   fun createEventConsumer(queryId: String, schema:Schema): QueryEventConsumer
}
