package com.orbitalhq.query.tracing

interface TracingEventSink {
   fun emitEvent(source: SpanEventSource, event: TracingEvent)
}
object NoopTracingEventSink : TracingEventSink {
   override fun emitEvent(source: SpanEventSource, event: TracingEvent) {
   }
}

/**
 * For testing.
 * Probably easier to call QueryContextEventBroker.withTestTraceSpan()
 * which returns the broker and sink already wired up
 */
class CollectingEventSink : TracingEventSink {
   private val events = mutableListOf<Pair<SpanEventSource,TracingEvent>>()
   override fun emitEvent(source: SpanEventSource, event: TracingEvent) {
      events.add(Pair(source,event))
   }

   val collectedEvents: List<TracingEvent>
      get() {
         return events.map { it.second }
      }

}
