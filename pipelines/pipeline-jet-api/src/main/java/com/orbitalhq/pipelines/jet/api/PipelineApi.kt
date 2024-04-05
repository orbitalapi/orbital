package com.orbitalhq.pipelines.jet.api

import com.orbitalhq.UriSafePackageIdentifier
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.api.streams.StreamStatusUpdateRequest
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import reactor.core.publisher.Mono

interface PipelineApi {
   @PostExchange("/api/pipelines/{packageIdentifier}")
   fun submitPipeline(
      @PathVariable("packageIdentifier") packageUri: UriSafePackageIdentifier,
      @RequestBody pipelineSpec: PipelineSpec<*, *>
   ): Mono<SubmittedPipeline>

   @GetExchange("/api/pipelines")
   fun getPipelines(): Mono<List<RunningPipelineSummary>>

   @GetExchange("/api/pipelines/{pipelineSpecId}")
   fun getPipeline(@PathVariable("pipelineSpecId") pipelineSpecId: String): Mono<RunningPipelineSummary>;

   @DeleteExchange("/api/pipelines/{pipelineSpecId}")
   fun deletePipeline(@PathVariable("pipelineSpecId") pipelineSpecId: String): Mono<PipelineStatus>

   @PostExchange("/api/streams/{streamName}/status")
   fun updateStreamStatus(
      @PathVariable("streamName") streamName: String,
      @RequestBody request: StreamStatusUpdateRequest
   ): Mono<StreamStatus>

   @GetExchange("/api/streams/{streamName}/status")
   fun getStreamStatus(
      @PathVariable("streamName") streamName: String,
   ): Mono<StreamStatus>

}

