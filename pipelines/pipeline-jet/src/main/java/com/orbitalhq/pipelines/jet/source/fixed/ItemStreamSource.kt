package com.orbitalhq.pipelines.jet.source.fixed

import com.hazelcast.jet.pipeline.StreamSource
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

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
   override val description: String = "Jet stream source"
}

// Not a production SourceBuilder, so not declared as a Component
class ItemStreamSourceBuilder : PipelineSourceBuilder<ItemStreamSourceSpec> {
   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is ItemStreamSourceSpec
   }

   override fun build(
      pipelineSpec: PipelineSpec<ItemStreamSourceSpec, *>,
      inputType: Type?
   ): StreamSource<MessageContentProvider> {
      return pipelineSpec.input.source as StreamSource<MessageContentProvider>
   }

   override fun getEmittedType(pipelineSpec: PipelineSpec<ItemStreamSourceSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.typeName
   }

}
