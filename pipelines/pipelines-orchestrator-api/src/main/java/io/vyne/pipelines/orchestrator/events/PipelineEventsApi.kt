package io.vyne.pipelines.orchestrator.events

import io.vyne.pipelines.orchestrator.events.PipelineEvent
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("pipeline-orchestrator")
interface PipelineEventsApi {

   @PostMapping("/pipelines/events")
   fun publish(@RequestBody event: PipelineEvent)
}
