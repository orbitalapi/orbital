package io.vyne.pipelines.jet.sink.list

import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.spring.context.SpringAware
import io.vyne.models.TypedInstance
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.TypedInstanceContentProvider
import io.vyne.pipelines.jet.sink.PipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import org.springframework.stereotype.Component
import javax.annotation.Resource

data class ListSinkSpec(val outputTypeName: String) :
   PipelineTransportSpec {
   constructor(outputType: QualifiedName) : this(outputType.parameterizedName)

   override val type: PipelineTransportType = "list"
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val props: Map<String, Any> = emptyMap()
   override val description: String = "List sink"


//   val outputType: QualifiedName
//      get() {
//         return outputTypeName.fqn()
//      }
}

class ListSinkBuilder : PipelineSinkBuilder<ListSinkSpec> {
   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.output is ListSinkSpec

   override fun build(pipelineSpec: PipelineSpec<*, ListSinkSpec>): Sink<MessageContentProvider> {
      return SinkBuilder
         .sinkBuilder("list-sink") { _ -> ListSinkContext() }
         .receiveFn { context: ListSinkContext, item: MessageContentProvider ->
            context.target.add(item)
         }
         .build()

   }

   override fun getRequiredType(pipelineSpec: PipelineSpec<*, ListSinkSpec>, schema: Schema) =
      pipelineSpec.output.outputTypeName.fqn()
}

@SpringAware
class ListSinkContext {
   @Resource(name = ListSinkTarget.NAME)
   lateinit var target: ListSinkTarget
}

@Component
open class ListSinkTarget(val name: String = "unnamed") {
   companion object {
      const val NAME = "link-sink-target"
   }

   val list: MutableList<MessageContentProvider> = mutableListOf()

   val size: Int
      get() {
         return list.size
      }

   fun add(item: MessageContentProvider) = list.add(item)
   fun first(): TypedInstance {
      val first = this.list.first() as TypedInstanceContentProvider
      return first.content
   }

   fun firstRawValue(): Any? {
      return first().toRawObject()
   }
}
