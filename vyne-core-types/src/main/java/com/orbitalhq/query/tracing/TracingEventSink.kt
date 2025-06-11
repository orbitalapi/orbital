package com.orbitalhq.query.tracing

interface TracingEventSink {
   fun emitEvent(event: TracingEvent)
}
object NoopTracingEventSink : TracingEventSink {
   override fun emitEvent(event: TracingEvent) {
   }
}
