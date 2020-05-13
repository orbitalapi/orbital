package io.vyne.pipelines.orchestrator.runners

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.runner.PipelineInstanceReference
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("pipeline-runner")
interface PipelineRunnerApi {

   /**
    * This methods takes a String and not a Pipeline as input. Explanation:
    * As soon as we cast the input string to anything more concrete, we lose the raw input which is what the downstream consumer requires.
    * If we use Pipeline, we might not send the full pipeline description to the runner.
    */
   @PostMapping("/runner/pipelines", consumes = ["application/json"])
   fun submitPipeline(@RequestBody pipeline: String): PipelineInstanceReference
}
