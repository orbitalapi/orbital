package io.vyne.pipelines.jet.api

import io.vyne.pipelines.jet.api.transport.PipelineSpec
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

/**
 * Playtika reactive feign client implementation uses the value passed into 'url' attribute when it is not an 'empty' string
 * and does not try to resolve the end point through discovery service lookup. We leverage this in our integration tests
 * (see VyneQuerySecurityIntegrationTest ) so that we can 'mock' Cask Server through a fake server, e.g. WireMock.
 */
@ReactiveFeignClient("\${vyne.pipelinesJetRunner.name:pipeline-jet-runner}", url = "\${vyne.pipelinesJetRunner.url:}")
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

