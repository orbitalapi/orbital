package io.vyne.pipelines.jet.api

import io.vyne.pipelines.PipelineSpec
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.pipelinesJetRunner.name:pipeline-jet-runner}")
interface PipelineApi {
   @PostMapping("/api/pipelines")
   fun submitPipeline(@RequestBody pipelineSpec: PipelineSpec<*, *>): Mono<SubmittedPipeline>

   @GetMapping("/api/pipelines")
   fun getPipelines(): Mono<List<RunningPipelineSummary>>

   @GetMapping("/api/pipelines/{pipelineSpecId}")
   fun getPipeline(@PathVariable("pipelineSpecId") pipelineSpecId:String):Mono<RunningPipelineSummary>;

   @DeleteMapping("/api/pipelines/{pipelineSpecId}")
   fun deletePipeline(@PathVariable("pipelineSpecId") pipelineSpecId: String): Mono<PipelineStatus>
}

