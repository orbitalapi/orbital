package io.vyne.pipelines.runner.transport

import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransport
import io.vyne.pipelines.PipelineTransportSpec
import org.springframework.stereotype.Component


@Component
class PipelineTransportFactory(private val builders: List<PipelineTransportBuilder<out PipelineTransportSpec, out PipelineTransport>>) {

   fun buildInput(spec: PipelineTransportSpec, logger: PipelineLogger): PipelineInputTransport {
      return builders
         .filterIsInstance<PipelineInputTransportBuilder<PipelineTransportSpec>>()
         .first { it.canBuild(spec) }
         .build(spec, logger, this)
   }

   fun buildOutput(spec: PipelineTransportSpec, logger: PipelineLogger): PipelineOutputTransport {
      return builders
         .filterIsInstance<PipelineOutputTransportBuilder<PipelineTransportSpec>>()
         .firstOrNull { it.canBuild(spec) }
         ?.build(spec, logger, this)
         ?: error("No builder found capable of building spec of type ${spec.direction} ${spec.type}")
   }

}

interface PipelineTransportBuilder<S : PipelineTransportSpec, T: PipelineTransport> {
   fun canBuild(spec: PipelineTransportSpec): Boolean
   fun build(spec: S, logger: PipelineLogger, transportFactory: PipelineTransportFactory): T
}

interface PipelineInputTransportBuilder<S: PipelineTransportSpec> : PipelineTransportBuilder<S, PipelineInputTransport>
interface PipelineOutputTransportBuilder<S: PipelineTransportSpec> : PipelineTransportBuilder<S, PipelineOutputTransport>
