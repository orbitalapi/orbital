package io.vyne.pipelines.runner

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineOutputTransport
import reactor.core.Disposable

class PipelineInstance(
   private val spec: Pipeline,
   private val activePipeline: Disposable,
   val input:PipelineInputTransport,
   val output:PipelineOutputTransport
)
