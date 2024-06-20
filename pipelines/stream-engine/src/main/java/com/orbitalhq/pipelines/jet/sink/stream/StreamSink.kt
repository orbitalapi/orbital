package com.orbitalhq.pipelines.jet.sink.stream

import com.hazelcast.jet.datamodel.WindowResult
import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.spring.context.SpringAware
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType
import com.orbitalhq.pipelines.jet.api.transport.TypedInstanceContentProvider
import com.orbitalhq.pipelines.jet.api.transport.WindowingPipelineTransportSpec
import com.orbitalhq.pipelines.jet.sink.WindowingPipelineSinkBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * Exists only for testing purposes.
 */
data class StreamSinkSpec(
   val outputTypeName: String,
   val target: String = "default",
   override val windowDurationMs: Long = 1000
) :
   WindowingPipelineTransportSpec {
   constructor(outputType: QualifiedName) : this(outputType.parameterizedName)
   constructor(outputType: QualifiedName, target: String) : this(outputType.parameterizedName, target)

   override val type: PipelineTransportType = "stream"
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val description: String = "Stream sink"
}

class StreamSinkBuilder : WindowingPipelineSinkBuilder<StreamSinkSpec> {
   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is StreamSinkSpec

   override fun getRequiredType(pipelineTransportSpec: StreamSinkSpec, schema: Schema) =
      pipelineTransportSpec.outputTypeName.fqn()

   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: StreamSinkSpec
   ): Sink<WindowResult<List<MessageContentProvider>>> {
      return SinkBuilder
         .sinkBuilder("stream-sink") { StreamSinkContext() }
         .receiveFn { context: StreamSinkContext, message: WindowResult<List<MessageContentProvider>> ->
            message.result().forEach {
               context.targetContainer.getOrCreateTarget(pipelineTransportSpec.target).add(it)
            }

         }
         .build()

   }
}

@SpringAware
class StreamSinkContext {
   @Resource(name = StreamSinkTargetContainer.NAME)
   lateinit var targetContainer: StreamSinkTargetContainer
}

@Component
class StreamSinkTargetContainer(val name: String = "unnamed") {
   companion object {
      const val NAME = "stream-sink-target"
   }

   val targets: MutableMap<String, StreamSinkTarget> = mutableMapOf()

   fun getOrCreateTarget(name: String): StreamSinkTarget {
      if (!targets.containsKey(name)) {
         targets[name] = StreamSinkTarget()
      }
      return targets[name]!!
   }
}

class StreamSinkTarget {
   val list: MutableList<MessageContentProvider> = mutableListOf()

   val size: Int
      get() {
         return list.size
      }

   fun add(item: MessageContentProvider) {
      list.add(item)
   }

   fun first(): TypedInstance {
      val first = this.list.first() as TypedInstanceContentProvider
      return first.content
   }

   fun firstRawValue(): Any? {
      return first().toRawObject()
   }
}
