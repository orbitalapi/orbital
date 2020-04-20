package io.vyne.pipelines.runner.events

import io.vyne.pipelines.orchestrator.events.PipelineEvent


interface PipelineEventSink {
   fun publish(event: PipelineEvent)
}

// useful for testing
class CollectingEventSink : PipelineEventSink {
   private val events = mutableListOf<PipelineEvent>()
   override fun publish(event: PipelineEvent) {
      events.add(event)
   }
}
