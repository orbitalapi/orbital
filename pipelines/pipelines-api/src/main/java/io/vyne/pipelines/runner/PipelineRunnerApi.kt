package io.vyne.pipelines.runner

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.pipelines.Pipeline
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import java.time.Instant

@ReactiveFeignClient("\${vyne.pipelineRunnerService.name:pipeline-runner}")
interface PipelineRunnerApi {
   @PostMapping("/api/pipelines")
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
