package io.vyne.pipelines.runner.events

import io.vyne.pipelines.orchestrator.events.PipelineEvent
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import org.springframework.stereotype.Component

@Component
class HttpEventSink(private val eventsApi: PipelineEventsApi) : PipelineEventSink {
   override fun publish(event: PipelineEvent) {
      eventsApi.publish(event)
   }

}
