package io.vyne.query

import io.vyne.models.OperationResult
import io.vyne.schemas.RemoteOperation


/**
 * Lightweight interface to allow components used throughout execution of a query
 * to send messages back up to the QueryContext.
 *
 * It's up to the query context what to do with these messages.  It may ignore them,
 * or redistribute them.  Callers should not make any assumptions about the impact of calling these methods.
 *
 * Using an interface here as we don't always actually have a query context.
 */
interface QueryContextEventDispatcher {
   /**
    * Signals an incremental update to the estimated record count, as reported by the provided operation.
    * This is populated by services setting the HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT header
    * in their response to Vyne.
    */
   fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {}

   /**
    * Request that this query cancel.
    */
   fun requestCancel() {}


   fun reportRemoteOperationInvoked(operation: OperationResult, queryId: String) {}
}
