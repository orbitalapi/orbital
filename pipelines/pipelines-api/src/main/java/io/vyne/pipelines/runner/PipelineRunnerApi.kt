package io.vyne.pipelines.runner

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineTransportHealthMonitor
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono
import java.time.Instant

@ReactiveFeignClient("\${vyne.pipelineRunnerService.name:pipeline-runner}")
interface PipelineRunnerApi {
   @PostMapping("/api/pipelines")
   fun submitPipeline(@RequestBody pipeline: Pipeline): Mono<PipelineInstanceReference>

   @DeleteMapping("/api/pipelines/{pipelineName}")
   fun removePipeline(@PathVariable("pipelineName") pipelineName: String): Mono<PipelineStatusUpdate>
}

data class PipelineStatusUpdate(
   val name: String,
   val status: PipelineTransportHealthMonitor.PipelineTransportStatus
)

@JsonDeserialize(`as` = SimplePipelineInstance::class)
interface PipelineInstanceReference {
   val spec: Pipeline
   val startedTimestamp: Instant
}

data class SimplePipelineInstance(
   override val spec: Pipeline,
   override val startedTimestamp: Instant
) : PipelineInstanceReference
