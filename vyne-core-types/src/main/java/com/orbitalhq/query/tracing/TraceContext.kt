package com.orbitalhq.query.tracing

import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type

data class TraceContext(
   /**
    * The query Id. Do not use clientQueryId here
    */
   val queryId: String,
   /**
    * TraceId. Never null.
    * If not provided upstream, then create one.
    * A trace is the top-level, longest-lived concept in tracing, and encapsulates all the
    * activities in a full operation.
    *
    * Companies using OTEL or e2e tracing will supply this - sometimes on an HTTP header. If not, and you're the first
    * event in the chain, create a new tracing id
    */
   val traceId: String,
   /**
    * Represents this specific activity.
    * A request / response would share the same spanId.
    *
    * (Http call request/response, Kafka subscription, etc.)
    * Multiple events can relate to a single Span
    * For things that are long-lived, (like Kafka), you'd have a single span for the entire subscription,
    * with multiple tracing events.
    */
   val spanId: String,
   val parentSpanId: String?,
   val eventSink: TracingEventSink
) {
   companion object {
      fun noOp():TraceContext {
         return newTrace("no-op", NoopTracingEventSink)
      }
      /**
       * Creates a new root trace context (no parent)
       */
      fun newTrace(queryId: String, eventSink: TracingEventSink): TraceContext {
         return TraceContext(
            traceId = TracingEvent.newTraceId(),
            spanId = TracingEvent.newSpanId(),
            parentSpanId = null,
            queryId = queryId,
            eventSink = eventSink
         )
      }

      /**
       * Creates trace context from existing OTEL headers/context
       */
      fun forTraceId(
         traceId: String,
         queryId: String,
         eventSink: TracingEventSink
      ): TraceContext {
         return TraceContext(
            traceId = traceId,
            spanId = TracingEvent.newSpanId(),
            parentSpanId = null,
            queryId = queryId,
            eventSink = eventSink
         )
      }
   }
   fun createChildSpan(): TraceContext {
      return copy(
         spanId = TracingEvent.newSpanId(),
         parentSpanId = this.spanId,
      )
   }

   fun emitEvent(
       kind: TracingEventKind,
       spanState: SpanState,
       exchangeMetadata: TracingEventExchangeMetadata,
       /**
        * The emitter provides identifying information (like Service, Operation),
        * of where the event is being emitter for. This allows us to contextually exclude
        * payloads from being captured by allowing annotations on services / operations / payload types.
        */
       emitter: EventEmitter
   ) {
      val event = TracingEvent(
          queryId = queryId,
          traceId = traceId,
          spanId = spanId,
          parentSpanId = parentSpanId,
          tracingEventKind = kind,
          spanState = spanState,
          exchangeMetadata = exchangeMetadata
      )
      this.eventSink.emitEvent(event)
   }
}

/**
 * Provides metadata through to event builders
 * to allow for contextual filtering (or prevention) of events.
 * Allows us to control payload filtering by annotation on operations or operation
 * payload types
 */
sealed interface EventEmitter

data class OperationEventEmitter(
   val service: Service,
   val operation: RemoteOperation,
   val payloadType: Type
) : EventEmitter
