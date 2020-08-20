package io.vyne.pipelines.orchestrator.events

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("\${vyne.pipelinesOrchestratorService.name:pipelines-orchestrator}")
interface PipelineEventsApi {

   @PostMapping("/api/pipelines/events")
   fun publish(@RequestBody event: PipelineEvent)
}
