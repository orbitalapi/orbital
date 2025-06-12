package com.orbitalhq.query.tracing

interface TracingEventSink {
   fun emitEvent(source: SpanEventSource, event: TracingEvent)
}
object NoopTracingEventSink : TracingEventSink {
   override fun emitEvent(source: SpanEventSource, event: TracingEvent) {
   }
}
