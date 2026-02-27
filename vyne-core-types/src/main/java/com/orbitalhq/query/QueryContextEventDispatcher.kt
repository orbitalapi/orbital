package com.orbitalhq.query

import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.TraceSpan
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service


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
   fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int)

   /**
    * Request that this query cancel.
    */
   fun requestCancel()


   /**
    * Called when an operation is not invoked because it's result is already cached,
    * but the inputs that were provided came from different sources than the original cached operation.
    *
    * The net effect is that while the value that's returned is the same, the lineage
    * for that value now has two paths - so lineage related tooling needs to capture this
    */
   fun reportCachedOperationWithUniquePathObserved(operation: OperationResultReference, queryId: String) {}
   fun reportRemoteOperationInvoked(operation: OperationResult, queryId: String)

   val queryErrorPublisher: StreamErrorPublisher

   val traceSpan: TraceSpan

   fun createSpan(): TraceSpan {
      return this.traceSpan.createChild()
   }

   /**
    * Helper function, just a wrapper around createSpan() that reduces boilerplate
    */
   fun createOperationTraceSpan(
      service: Service,
      operation: RemoteOperation,
      /**
       * A human readable name for the resource that this event relates to.
       * Could be a table name, topic, url, etc.
       */
      eventResourceName: String,

      remoteCallId: String
   ): OperationTraceSpan {
      return OperationTraceSpan(this.createSpan(), service, operation, eventResourceName, remoteCallId)
   }

// TODO: This didn't get implemented, as passing tags around was too messy / too easy
   // to miss.
   // But, the desire is to be able to see for persistentStreams / endpoint queries the number
   // of results emitted., to allow monitoring to track end-to-end metrics attributed to a specific
   // query.
//   /**
//    * These metric tags are propagated into child queries.
//    * They should be used for things that you always want attributed to
//    * the overall query - like tracking operation calls.
//    *
//    * In general, these should be passed onto new QueryContexts that are created.
//    * By contrast, metric tags that are passed on find() calls are scoped differently,
//    * so we can treat inner queries separately
//    *
//    */
//   val endToEndMetricTags:MetricTags

}

/**
 * A way for a QueryContextEventDispatcher to provide access to the underlying schema.
 * Used in Operations, (specifically KAfka), where QueryContextEventDispatcher is a
 * QueryContext,  But need to understand the usecases
 * where QueryContextEventDispatcher is passed to an invoker, but isn't a QueryContext
 */
interface QueryContextSchemaProvider {
   val schema: Schema
   fun populateResponseHeaders(): Map<String, List<String>>
}
