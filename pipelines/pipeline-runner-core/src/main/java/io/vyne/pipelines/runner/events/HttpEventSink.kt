package io.vyne.pipelines.runner.events

import io.vyne.pipelines.orchestrator.events.PipelineEvent
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.utils.log
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class HttpEventSink(private val eventsApi: PipelineEventsApi) : PipelineEventSink {
   override fun publish(event: PipelineEvent) {
      Mono.fromCallable { eventsApi.publish(event) }
         .doOnError { log().error("Could not publish http event", it) }
         .subscribe()
   }

}
