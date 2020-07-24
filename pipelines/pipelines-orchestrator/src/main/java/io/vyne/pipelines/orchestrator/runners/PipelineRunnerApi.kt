package io.vyne.pipelines.orchestrator.runners

import io.vyne.pipelines.orchestrator.OperationResult
import io.vyne.pipelines.runner.PipelineInstanceReference
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient("\${vyne.pipelineRunnerService.name:pipeline-runner}")
interface PipelineRunnerApi {

   @PostMapping("/api/pipelines", consumes = ["application/json"])
   fun submitPipeline(@RequestBody pipeline: String): PipelineInstanceReference

   // TODO might be removed as part of https://projects.notional.uk/youtrack/issue/LENS-159
   // See TestharnessController.kt for details
   @PostMapping("/testharness/send2kafka/{kafkaTopic}")
   fun submitMessage(@PathVariable("kafkaTopic") kafkaTopic: String,
                     @RequestParam("kafkaHost", defaultValue = "kafka:9092") kafkaHost: String,
                     @RequestBody kafkaJsonMessage: String): OperationResult
}
