package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.runner.PipelineInstanceReference
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@RestController
class TestharnessController(val pipelineRunnerApi: PipelineRunnerApiFeign) {
    data class OperationResult(val success: Boolean, val message: String)

    @PostMapping("/testharness/send2kafka/{kafkaTopic}")
    @CrossOrigin
    fun sendKafkaMessage(@PathVariable("kafkaTopic") kafkaTopic: String,
                         @RequestParam("kafkaHost", defaultValue = "kafka:9092") kafkaHost: String,
                         @RequestBody kafkaJsonMessage: String): Any {
       return pipelineRunnerApi.submitMessage(kafkaTopic, kafkaHost, kafkaJsonMessage)
    }

}

@FeignClient("pipeline-runner")
interface PipelineRunnerApiFeign {

   @PostMapping("/testharness/send2kafka/{kafkaTopic}")
   fun submitMessage(@PathVariable("kafkaTopic") kafkaTopic: String,
                     @RequestParam("kafkaHost", defaultValue = "kafka:9092") kafkaHost: String,
                     @RequestBody kafkaJsonMessage: String): PipelineInstanceReference
}

