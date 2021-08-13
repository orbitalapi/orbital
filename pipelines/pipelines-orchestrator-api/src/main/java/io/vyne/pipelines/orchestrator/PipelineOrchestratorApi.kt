package io.vyne.pipelines.orchestrator

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.pipelinesOrchestratorService.name:pipelines-orchestrator}")
interface PipelinesOrchestratorApi {
   @PostMapping("/api/pipelines")
   fun submitPipeline(@RequestBody pipelineDescription: String): Mono<PipelineStateSnapshot>

   @GetMapping("/api/runners")
   fun getRunners(): Mono<List<PipelineRunnerInstance>>

   @GetMapping("/api/pipelines")
   fun getPipelines(): Mono<List<PipelineStateSnapshot>>

}

data class PipelineStateSnapshot(
   val name: String,
   val pipelineDescription: String,
   var instance: PipelineRunnerInstance?,
   var state: PipelineState,
   var info: String = ""
)

enum class PipelineState {

   // Pipeline has been scheduled for assignment to a runner
   SCHEDULED,

   // Pipeline has been submitting to a runner, and we're waiting for startup
   STARTING,

   // Pipeline is running on a runner. For now, running means just that the pipeline has been sent to a runner, and the runner acknowledged the pipeline (wrote it in its metadata)
   RUNNING
}

data class PipelineRunnerInstance(val instanceId: String, val uri: String)
