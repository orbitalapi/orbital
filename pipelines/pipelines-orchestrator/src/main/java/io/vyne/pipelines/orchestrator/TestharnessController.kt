package io.vyne.pipelines.orchestrator

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

// Temporary Controller to send Kafka messages to runners.
// This allows us to ignore any CORS related issues in the browser when showing the Orchestrator UI
// This in only needed as currently, we have one UI for two purposes:
// - see the orchestator and runner states
// - send test kafka message
// which is convenient for testing / demoing
// TODO: Decide what to do with these UIs and remove/move this once decided
// https://projects.notional.uk/youtrack/issue/LENS-159

data class OperationResult(val success: Boolean, val message: String)

@RestController
class TestharnessController(val pipelineRunnerApi: PipelineRunnerApiFeign) {

    @PostMapping("/testharness/send2kafka/{kafkaTopic}")
    @CrossOrigin
    fun sendKafkaMessage(@PathVariable("kafkaTopic") kafkaTopic: String,
                         @RequestParam("kafkaHost", defaultValue = "kafka:9092") kafkaHost: String,
                         @RequestBody kafkaJsonMessage: String): OperationResult {
       return pipelineRunnerApi.submitMessage(kafkaTopic, kafkaHost, kafkaJsonMessage)
    }

}

@FeignClient("pipeline-runner")
interface PipelineRunnerApiFeign {

   @PostMapping("/testharness/send2kafka/{kafkaTopic}")
   fun submitMessage(@PathVariable("kafkaTopic") kafkaTopic: String,
                     @RequestParam("kafkaHost", defaultValue = "kafka:9092") kafkaHost: String,
                     @RequestBody kafkaJsonMessage: String): OperationResult
}

