package io.vyne.pipelines.runner

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.pipelines.Pipeline
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.time.Instant

@FeignClient("pipeline-runner")
interface PipelineRunnerApi {
   @PostMapping("/runner/pipelines")
   fun submitPipeline(@RequestBody pipeline: Pipeline): PipelineInstanceReference
}

@JsonDeserialize(`as` = SimplePipelineInstance::class)
interface PipelineInstanceReference {
   val spec: Pipeline
   val startedTimestamp: Instant
}

data class SimplePipelineInstance(
   override val spec: Pipeline,
   override val startedTimestamp: Instant
) : PipelineInstanceReference
