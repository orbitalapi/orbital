package io.vyne.pipelines.orchestrator.events

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.pipelinesOrchestratorService.name:pipelines-orchestrator}")
interface PipelineEventsApi {

   @PostMapping("/api/pipelines/events")
   fun publish(@RequestBody event: PipelineEvent): Mono<Void>
}
