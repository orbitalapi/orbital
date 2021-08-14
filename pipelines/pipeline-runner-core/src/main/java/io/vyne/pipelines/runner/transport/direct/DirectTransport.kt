package io.vyne.pipelines.runner.transport.direct

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.EmitterPipelineTransportHealthMonitor
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportHealthMonitor
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import reactor.core.publisher.Flux

/**
 * This type is primarily useful for testing, where the source and destination
 * are just fluxes
 */
object DirectTransport {
   const val TYPE: PipelineTransportType = "direct"
}

data class DirectTransportInputSpec(
   val source: Flux<PipelineInputMessage>,
   val messageType: VersionedTypeReference,
   override val props: Map<String, String> = emptyMap()
) : PipelineTransportSpec {
   override val type: PipelineTransportType = DirectTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val description: String = "Direct input"

}

class DirectInputBuilder : PipelineInputTransportBuilder<DirectTransportInputSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec.type == DirectTransport.TYPE
         && spec.direction == PipelineDirection.INPUT
   }

   override fun build(
      spec: DirectTransportInputSpec,
      logger: PipelineLogger,
      transportFactory: PipelineTransportFactory,
      pipeline: Pipeline
   ): PipelineInputTransport {
      return DirectInput(spec.source, spec.messageType)
   }
}

class DirectInput(override val feed: Flux<PipelineInputMessage>, private val messageType:VersionedTypeReference) : PipelineInputTransport {
   override val description: String = "Direct input"
   override fun type(schema: Schema): Type {
      return schema.type(messageType)
   }
}

data class DirectOutputSpec(val name: String = "Unnamed", val messageType: VersionedTypeReference) :
   PipelineTransportSpec {
   override val type: PipelineTransportType = DirectTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val props = emptyMap<String, String>()
   override val description: String = "Direct output"
}

class DirectOutputBuilder(val healthMonitor: PipelineTransportHealthMonitor = EmitterPipelineTransportHealthMonitor()) : PipelineOutputTransportBuilder<DirectOutputSpec> {
   val builtInstances = mutableListOf<DirectOutput>()
   fun clearAll() {
      builtInstances.clear()
   }
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec is DirectOutputSpec
   }

   override fun build(
      spec: DirectOutputSpec,
      logger: PipelineLogger,
      transportFactory: PipelineTransportFactory,
      pipeline: Pipeline
   ): PipelineOutputTransport {
      val output = DirectOutput(spec.name, spec.messageType, healthMonitor)
      builtInstances.add(output)
      return output
   }

}

class DirectOutput(val name: String, val typeName: VersionedTypeReference, override val healthMonitor: PipelineTransportHealthMonitor = EmitterPipelineTransportHealthMonitor()) : PipelineOutputTransport {
   val messages: MutableList<String> = mutableListOf()

   override val description: String = "Direct output"

   override fun type(schema: Schema): Type {
      return schema.type(typeName)
   }

   override fun write(message: MessageContentProvider, logger: PipelineLogger, schema: Schema) {
      this.messages.add(message.asString(logger))
   }

}
