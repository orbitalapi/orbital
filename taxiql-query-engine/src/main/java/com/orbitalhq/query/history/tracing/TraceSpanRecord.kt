package com.orbitalhq.query.history.tracing

import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import java.time.Duration
import java.time.Instant

/**
 * This is a span, as created after reading raw events from the db.
 * It's not the record that gets written to the db, or part of the event
 * emission process
 */
data class TraceSpanRecord(
   val spanId: String,
   val parentSpanId: String?,
   val events: List<TraceEventRow>,
   val children: MutableList<TraceSpanRecord> = mutableListOf(),
   val traceStartTime: Instant // The earliest timestamp from all events in the trace
) {
   /**
    * The first event, as determined purely by timestamps on events.
    * If the span start event is present, should match the
    * timestamp on the start event, but this is not guaranteed, as is
    * determined by the event producers.
    */
   val firstEventTimestamp: Instant = events.minByOrNull { it.timestamp }?.timestamp?.toInstant()
      ?: throw IllegalStateException("Span must have at least one event")

   /**
    * The last event, as determined purely by timestamps on events.
    * If the end event is present, should match the
    * timestamp on the end event, but this is not guaranteed, as is
    * determined by the event producers.
    */
   val lastEventTimestamp: Instant = events.maxByOrNull { it.timestamp }?.timestamp?.toInstant()
      ?: throw IllegalStateException("Span must have at least one event")

   /**
    * The timestamp on the start event, if present.
    * There should almost always be a start event, but as this is determined
    * by invokers emitting events correctly, it can't be guaranteed.
    */
   val spanStartTimestamp: Instant? = events
      .firstOrNull { it.spanState == SpanState.ACTIVE }
      ?.timestamp?.toInstant()

   /**
    * The timestamp on the end event, if present.
    * There should almost always be an end event, but as this is determined
    * by invokers emitting events correctly, it can't be guaranteed.
    */
   val spanEndTimestamp: Instant? = events
      .firstOrNull { it.spanState == SpanState.COMPLETE }
      ?.timestamp?.toInstant()

   /**
    * A verb, (as reported by the first event of the span), that provides a succinct description of
    * what this event was.
    * eg: "Subscribe", "Disconnect", "Receive", "Get", "Post", "Invoke", etc.
    */
   val eventVerb: String = events.minByOrNull { it.timestamp }?.eventVerb
      ?: throw IllegalStateException("Span must have at least one event")

   /**
    * A human readable name for the resource (as reported by the first event of the span) that this event relates to.
    * Could be a table name, topic, url, etc.
    */
   val eventResource: String = events.minByOrNull { it.timestamp }?.eventResource
      ?: throw IllegalStateException("Span must have at least one event")

   /**
    * The qualified name of the operation, (or if this was a taxi function),
    * the taxi function qualified name. Determined by the first event of the span
    */
   val eventSourceQualifiedName: String = events.minByOrNull { it.timestamp }?.eventSourceQualifiedName
      ?: throw IllegalStateException("Span must have at least one event")

   /**
    * The time in milliseconds that this span started, as an offset from the very first event
    * in the collection of trace records.
    */
   val offsetMs: Long = Duration.between(traceStartTime, firstEventTimestamp).toMillis()

   /**
    * How long this TraceSpanRecord lasted, as determined by the duration between
    * the first and last events in this span.
    */
   val durationMs: Long = Duration.between(firstEventTimestamp, lastEventTimestamp).toMillis()

   /**
    * Duration of the span based on explicit start/end events.
    * Returns null if either start or end timestamp is missing.
    */
   val spanDurationMs: Long? = if (spanStartTimestamp != null && spanEndTimestamp != null) {
      Duration.between(spanStartTimestamp, spanEndTimestamp).toMillis()
   } else null

   /**
    * Whether this span has both start and end events
    */
   val isComplete: Boolean = spanStartTimestamp != null && spanEndTimestamp != null

   /**
    * Whether this span has any error events
    */
   val hasErrors: Boolean = events.any { it.tracingEventKind == TracingEventKind.ERROR }

   fun flatten():List<TraceSpanRecord> = listOf(this) + children.flatMap { it.flatten() }
}
