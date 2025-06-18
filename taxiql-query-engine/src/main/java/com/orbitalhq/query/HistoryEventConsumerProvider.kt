package com.orbitalhq.query

import com.orbitalhq.query.tracing.NoopTracingEventSink
import com.orbitalhq.query.tracing.TracingEventSink
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema

interface HistoryEventConsumerProvider {
   fun createEventConsumer(queryId: String, schema:Schema): QueryEventConsumer
   fun createTraceEventSink(queryId: String, traceId: String, schema: Schema, queryOptions: QueryOptions): TracingEventSink {
      return NoopTracingEventSink
   }
}
