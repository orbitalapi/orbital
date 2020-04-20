package io.vyne.pipelines.runner

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineOutputTransport
import reactor.core.Disposable
import java.time.Instant

class PipelineInstance(
   override val spec: Pipeline,
   private val activePipeline: Disposable,
   override val startedTimestamp: Instant,
   @JsonIgnore
   val input: PipelineInputTransport,
   @JsonIgnore
   val output: PipelineOutputTransport
) : PipelineInstanceReference
