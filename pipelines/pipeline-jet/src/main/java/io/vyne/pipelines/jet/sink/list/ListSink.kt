package io.vyne.pipelines.jet.sink.list

import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.spring.context.SpringAware
import io.vyne.models.TypedInstance
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import org.springframework.stereotype.Component
import javax.annotation.Resource

data class ListSinkSpec(val outputTypeName: String, val target: String = "default") :
   PipelineTransportSpec {
   constructor(outputType: QualifiedName) : this(outputType.parameterizedName)
   constructor(outputType: QualifiedName, target: String) : this(outputType.parameterizedName, target)

   override val type: PipelineTransportType = "list"
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val description: String = "List sink"
}

class ListSinkBuilder : SingleMessagePipelineSinkBuilder<ListSinkSpec> {
   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is ListSinkSpec

   override fun getRequiredType(pipelineTransportSpec: ListSinkSpec, schema: Schema) =
      pipelineTransportSpec.outputTypeName.fqn()

   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: ListSinkSpec
   ): Sink<MessageContentProvider> {
      return SinkBuilder
         .sinkBuilder("list-sink") { ListSinkContext() }
         .receiveFn { context: ListSinkContext, item: MessageContentProvider ->
            context.targetContainer.getOrCreateTarget(pipelineTransportSpec.target).add(item)
         }
         .build()

   }
}

@SpringAware
class ListSinkContext {
   @Resource(name = ListSinkTargetContainer.NAME)
   lateinit var targetContainer: ListSinkTargetContainer
}

@Component
class ListSinkTargetContainer(val name: String = "unnamed") {
   companion object {
      const val NAME = "link-sink-target"
   }

   val targets: MutableMap<String, ListSinkTarget> = mutableMapOf()

   fun getOrCreateTarget(name: String): ListSinkTarget {
      if (!targets.containsKey(name)) {
         targets[name] = ListSinkTarget()
      }
      return targets[name]!!
   }
}

class ListSinkTarget {
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
