package io.vyne.pipelines.orchestrator.runners

import io.vyne.pipelines.orchestrator.OperationResult
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.pipelines.runner.PipelineStatusUpdate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

/**
 * This class is a dirty hack.
 * It's a facade in front of the REAL PipelineRunnerApi (from the pipelines-runner-api project), which has a strongly typed pipeline parameter.
 * I suspect this exists for somethign to do with issues in SerDe the pipeline spec.
 * However, I've just found this, and I'm prepping for a demo, so will have to come back.
 */
@ReactiveFeignClient("\${vyne.pipelineRunnerService.name:pipeline-runner}")
interface PipelineRunnerApi {

   @PostMapping("/api/pipelines", consumes = ["application/json"])
   fun submitPipeline(@RequestBody pipeline: String): Mono<PipelineInstanceReference>

   // TODO might be removed as part of https://projects.notional.uk/youtrack/issue/LENS-159
   // See TestharnessController.kt for details
   @PostMapping("/testharness/send2kafka/{kafkaTopic}")
   fun submitMessage(
      @PathVariable("kafkaTopic") kafkaTopic: String,
      @RequestParam("kafkaHost", defaultValue = "kafka:9092") kafkaHost: String,
      @RequestBody kafkaJsonMessage: String
   ): Mono<OperationResult>

   @DeleteMapping("/api/pipelines/{pipelineName}")
   fun removePipeline(@PathVariable("pipelineName") pipelineName: String): Mono<PipelineStatusUpdate>
}
