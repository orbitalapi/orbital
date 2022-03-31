package io.vyne.pipelines.jet.source.fixed

import com.hazelcast.jet.pipeline.StreamSource
import io.vyne.pipelines.jet.api.transport.*
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema

/**
 * A Vyne pipeline spec that works directly against a Hazelcast Jet
 * stream source.
 * Really only used in tests
 */
data class ItemStreamSourceSpec(
   val source: StreamSource<out MessageContentProvider>,
   val typeName: QualifiedName
) : PipelineTransportSpec {
   override val type: PipelineTransportType = "StreamSource"
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val props: Map<String, Any> = emptyMap()
   override val description: String = "Jet stream source"
}

// Not a production SourceBuilder, so not declared as a Component
class ItemStreamSourceBuilder : PipelineSourceBuilder<ItemStreamSourceSpec> {
   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is ItemStreamSourceSpec
   }

   override fun build(pipelineSpec: PipelineSpec<ItemStreamSourceSpec, *>): StreamSource<MessageContentProvider> {
      return pipelineSpec.input.source as StreamSource<MessageContentProvider>
   }

   override fun getEmittedType(pipelineSpec: PipelineSpec<ItemStreamSourceSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.typeName
   }

}
