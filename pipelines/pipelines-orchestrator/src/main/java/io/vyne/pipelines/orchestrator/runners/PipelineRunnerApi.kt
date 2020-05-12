package io.vyne.pipelines.orchestrator.runners

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("pipeline-runner")
interface PipelineRunnerApi {
   @PostMapping("/runner/pipelines", consumes = ["application/json"])
   fun submitPipeline(@RequestBody pipeline: String): String
}
