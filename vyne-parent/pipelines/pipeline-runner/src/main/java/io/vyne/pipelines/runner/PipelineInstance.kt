package io.vyne.pipelines.runner

import io.vyne.pipelines.Pipeline
import reactor.core.Disposable

class PipelineInstance(
   private val spec: Pipeline,
   private val activePipeline: Disposable
)
