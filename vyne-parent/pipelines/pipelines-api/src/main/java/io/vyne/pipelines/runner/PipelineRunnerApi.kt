package io.vyne.pipelines.runner

import io.vyne.pipelines.Pipeline
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.time.Instant

@FeignClient("pipeline-runner")
interface PipelineRunnerApi {
   @PostMapping("/runner/pipelines")
   fun submitPipeline(@RequestBody pipeline: Pipeline):PipelineInstanceReference
}

interface PipelineInstanceReference {
   val spec:Pipeline
   val startedTimestamp: Instant
}
