package com.orbitalhq.query.tracing

import kotlin.reflect.KClass

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

   fun collectedEventsWithMetadataOfType(vararg metadataTypes: KClass<out TracingEventExchangeMetadata>): List<TracingEvent> {
      return collectedEvents.filter { event ->
         metadataTypes.any { metadataType -> metadataType.isInstance(event.exchangeMetadata)  }
      }
   }
   val collectedEvents: List<TracingEvent>
      get() {
         return events.map { it.second }
      }

}
