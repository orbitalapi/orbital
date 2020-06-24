package io.vyne.pipelines.runner.transport

import io.vyne.pipelines.*
import org.springframework.stereotype.Component


@Component
class PipelineTransportFactory(private val builders: List<PipelineTransportBuilder<out PipelineTransportSpec, out PipelineTransort>>) {

   fun buildInput(spec: PipelineTransportSpec, logger: PipelineLogger): PipelineInputTransport {
      return builders
         .filterIsInstance<PipelineInputTransportBuilder<PipelineTransportSpec>>()
         .first { it.canBuild(spec) }
         .build(spec, logger)
   }

   fun buildOutput(spec: PipelineTransportSpec, logger: PipelineLogger): PipelineOutputTransport {
      return builders
         .filterIsInstance<PipelineOutputTransportBuilder<PipelineTransportSpec>>()
         .first { it.canBuild(spec) }
         .build(spec, logger)
   }

}

interface PipelineTransportBuilder<S : PipelineTransportSpec, T: PipelineTransort> {
   fun canBuild(spec: PipelineTransportSpec): Boolean
   fun build(spec: S, logger: PipelineLogger): T
}

interface PipelineInputTransportBuilder<S: PipelineTransportSpec> : PipelineTransportBuilder<S, PipelineInputTransport>
interface PipelineOutputTransportBuilder<S: PipelineTransportSpec> : PipelineTransportBuilder<S, PipelineOutputTransport>
