package io.vyne.pipelines.runner.transport

import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportSpec
import org.springframework.stereotype.Component


@Component
class PipelineTransportFactory(private val builders: List<PipelineTransportBuilder<out PipelineTransportSpec>>) {

   fun buildInput(spec: PipelineTransportSpec): PipelineInputTransport {
      return builders
         .filterIsInstance<PipelineInputTransportBuilder<PipelineTransportSpec>>()
         .first { it.canBuild(spec) }
         .build(spec)
   }

   fun buildOutput(spec: PipelineTransportSpec): PipelineOutputTransport {
      return builders
         .filterIsInstance<PipelineOutputTransportBuilder<PipelineTransportSpec>>()
         .first { it.canBuild(spec) }
         .build(spec)
   }
}

interface PipelineTransportBuilder<T : PipelineTransportSpec> {
   fun canBuild(spec: PipelineTransportSpec): Boolean
}

interface PipelineInputTransportBuilder<T : PipelineTransportSpec> : PipelineTransportBuilder<T> {
   fun build(spec: T): PipelineInputTransport
}

interface PipelineOutputTransportBuilder<T : PipelineTransportSpec> : PipelineTransportBuilder<T> {
   fun build(spec: T): PipelineOutputTransport
}
