package com.orbitalhq.query.tracing

import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

data class TraceSpan(
   private val traceContext: TraceContext,
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
   /**
    * Parent. Will be null for the root, otherwise should be populated
    */
   val parentSpanId: String?,
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val eventCount = AtomicInteger(0)
   fun createChild(): TraceSpan {
      val child = TraceSpan(traceContext, spanId = TracingEvent.newSpanId(), parentSpanId = this.spanId)
      if (eventCount.get() == 0 && parentSpanId != null) {
         logger.debug { "Child event span created without any events emitted - this could be a bug" }
      }
      return child
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
      spanEventSource: SpanEventSource,
      /**
       * A human readable name for the resource that this event relates to.
       * Could be a table name, topic, url, etc.
       */
      eventResource: String,
      /**
       * A verb, as determined by the event emitter, that provides a succinct description of
       * what this event was.
       * eg: "Subscribe", "Disconnect", "Receive", "Get", "Post", "Invoke", etc.
       */
      eventVerb: String,
      /**
       * The qualified name of the operation, (or if this was a taxi function),
       * the taxi function qualified name
       */
      eventSourceQualifiedName: String,
      linkedEventId: String? = null
   ): TracingEvent {
      this.eventCount.incrementAndGet()
      val event = TracingEvent(
         queryId = traceContext.queryId,
         traceId = traceContext.traceId,
         spanId = spanId,
         parentSpanId = parentSpanId,
         tracingEventKind = kind,
         spanState = spanState,
         exchangeMetadata = exchangeMetadata,
         eventResource = eventResource,
         eventVerb = eventVerb,
         eventSourceQualifiedName = eventSourceQualifiedName,
         linkedEventId = linkedEventId
      )
      this.traceContext.emitEvent(spanEventSource, event)
      return event
   }
}


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
   private val eventSink: TracingEventSink
) {
   val rootSpan: TraceSpan = TraceSpan(
      traceContext = this,
      spanId = TracingEvent.newSpanId(),
      parentSpanId = null
   )

   companion object {
      fun noOp(): TraceContext {
         return newTrace("no-op", NoopTracingEventSink)
      }

      /**
       * Creates a new root trace context (no parent)
       */
      fun newTrace(queryId: String, eventSink: TracingEventSink): TraceContext {
         return TraceContext(
            traceId = TracingEvent.newTraceId(),
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
            queryId = queryId,
            eventSink = eventSink
         )
      }
   }

   fun emitEvent(
      /**
       * The emitter provides identifying information (like Service, Operation),
       * of where the event is being emitter for. This allows us to contextually exclude
       * payloads from being captured by allowing annotations on services / operations / payload types.
       */
      spanEventSource: SpanEventSource,
      event: TracingEvent
   ) {
      this.eventSink.emitEvent(spanEventSource, event)
   }
}

/**
 * Provides metadata through to event builders
 * to allow for contextual filtering (or prevention) of events.
 * Allows us to control payload filtering by annotation on operations or operation
 * payload types
 */
sealed interface SpanEventSource

data object QueryEngineSpanEventSource : SpanEventSource {
   const val QUERY_ENGINE_RESOURCE = "Query engine"
}

data class OperationSpanEventSource(
   val service: Service,
   val operation: RemoteOperation,
   val payloadType: Type?
) : SpanEventSource {
}

/**
 * This is syntactic sugar to reduce boilerplate
 * when emitting events.
 */
class OperationTraceSpan(
   private val traceSpan: TraceSpan,
   private val service: Service,
   private val operation: RemoteOperation,
   /**
    * A human readable name for the resource that this event relates to.
    * Could be a table name, topic, url, etc.
    */
   private val eventResourceName: String
) {
   fun emitEvent(
      kind: TracingEventKind,
      spanState: SpanState,
      /**
       * The payload of the request / response, if applicable.
       * If passed, this will be used to determine event emission behaviour
       * (based on annotations present on the type).
       */
      payloadType: Type?,
      exchangeMetadata: TracingEventExchangeMetadata,
      /**
       * A verb, as determined by the event emitter, that provides a succinct description of
       * what this event was.
       * eg: "Subscribe", "Disconnect", "Receive", "Get", "Post", "Invoke", etc.
       */
      verb: String,

      /**
       * Indicates that this event was triggered by the provided event.
       * Normally used when linking the start of a projection to a
       * source message
       */
      linkedEventId: String? = null
   ): TracingEvent = traceSpan.emitEvent(
      kind,
      spanState,
      exchangeMetadata,
      OperationSpanEventSource(service, operation, payloadType),
      eventSourceQualifiedName = operation.qualifiedName.fullyQualifiedName,
      eventResource = eventResourceName,
      eventVerb = verb,
      linkedEventId = linkedEventId
   )
}
