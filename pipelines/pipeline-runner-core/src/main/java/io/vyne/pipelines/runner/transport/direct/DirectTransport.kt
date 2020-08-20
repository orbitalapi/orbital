package io.vyne.pipelines.runner.transport.direct

import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import reactor.core.publisher.Flux
import java.io.InputStream

/**
 * This type is primarily useful for testing, where the source and destination
 * are just fluxes
 */
object DirectTransport {
   const val TYPE: PipelineTransportType = "direct"
}

data class DirectTransportInputSpec(val source: Flux<PipelineInputMessage>, override val props: Map<String, String>? = null) : PipelineTransportSpec {
   override val type: PipelineTransportType = DirectTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val targetType: VersionedTypeReference
      get() = TODO("Not yet implemented")
   override val description: String = "Direct input"

}

class DirectInputBuilder : PipelineInputTransportBuilder<DirectTransportInputSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec.type == DirectTransport.TYPE
         && spec.direction == PipelineDirection.INPUT
   }

   override fun build(spec: DirectTransportInputSpec, logger: PipelineLogger, transportFactory: PipelineTransportFactory): PipelineInputTransport {
      return DirectInput(spec.source)
   }
}

class DirectInput(override val feed: Flux<PipelineInputMessage>) : PipelineInputTransport {
   override val description: String = "Direct input"
}

data class DirectOutputSpec(val name:String = "Unnamed") : PipelineTransportSpec {
   override val type: PipelineTransportType = DirectTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val targetType: VersionedTypeReference
      get() = TODO("Not yet implemented")
   override val props = emptyMap<String, String>()
   override val description: String = "Direct output"
}

class DirectOutputBuilder : PipelineOutputTransportBuilder<DirectOutputSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec is DirectOutputSpec
   }

   override fun build(spec: DirectOutputSpec, logger: PipelineLogger, transportFactory: PipelineTransportFactory): PipelineOutputTransport {
      return DirectOutput(spec.name)
   }

}

class DirectOutput(val name:String) : PipelineOutputTransport {
   val messages: MutableList<String> = mutableListOf()

   override val description: String = "Direct output"

   override val healthMonitor = EmitterPipelineTransportHealthMonitor()

   override val type: VersionedTypeReference
      get() = TODO("Not yet implemented")

   override fun write(message: MessageContentProvider, logger: PipelineLogger) {
      this.messages.add(message.asString(logger))
   }

}
