package io.vyne.queryService.pipelines

import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.queryService.utils.handleFeignErrors
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Query server facade which forwards requests onto the pipeline orchestrator
 */
@RestController
class PipelineServiceFacade(private val pipelineApi: PipelineApi) {

   @PostMapping("/api/pipelines")
   fun submitPipeline(@RequestBody pipelineSpec: PipelineSpec<*, *>): Mono<SubmittedPipeline> =
      handleFeignErrors {
         pipelineApi.submitPipeline(pipelineSpec)
      }

   @GetMapping("/api/pipelines")
   fun getPipelines(): Mono<List<RunningPipelineSummary>> = handleFeignErrors { pipelineApi.getPipelines() }

   @GetMapping("/api/pipelines/{pipelineSpecId}")
   fun getPipeline(@PathVariable("pipelineSpecId") pipelineSpecId:String):Mono<RunningPipelineSummary>  = handleFeignErrors {
      pipelineApi.getPipeline(pipelineSpecId)
   }

   @DeleteMapping("/api/pipelines/{pipelineName}")
   fun removePipeline(@PathVariable("pipelineName") pipelineName: String) = handleFeignErrors { pipelineApi.deletePipeline(pipelineName) }
}


