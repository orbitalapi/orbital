package com.orbitalhq.history

import com.orbitalhq.query.QueryErrorStreamEvent
import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.history.QueryErrorEventRow
import com.orbitalhq.query.history.QueryResultRow
import com.orbitalhq.query.history.RemoteCallResponse
import com.orbitalhq.query.history.TraceEventRow
import com.orbitalhq.query.tracing.TracingEvent

/**
 * When a query executes, we capture lots of events related
 * to observability - such as request/response to-from services,
 * payloads and lineage.
 *
 * These events are emitted to a QueryObservabilityWriter
 */
interface QueryObservabilityWriter {

   fun storeResultRow(resultRow: QueryResultRow)
   fun storeRemoteCallResponse(remoteCallResponse: RemoteCallResponse)
   fun storeLineageRecord(lineageRecord: LineageRecord)
   fun storeErrorEvent(event: QueryErrorEventRow)
   fun storeTraceEvent(event: TraceEventRow)
}
