package com.orbitalhq.pipelines.jet.source.fixed

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.MessageSourceWithGroupId
import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType
import com.orbitalhq.pipelines.jet.api.transport.StringContentProvider
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.pipelines.jet.source.PipelineSourceType
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

/**
 * A pipeline batch source with fixed items.
 * Intended for tests only.
 */
data class BatchItemsSourceSpec(
   val items: List<String>,
   val typeName: QualifiedName,
   val groupId: String? = null
) : PipelineTransportSpec {
   override val type: PipelineTransportType = "Batch"
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val description: String = "Batch input"
}


data class BatchMessageSourceMetadata(override val groupId: String) : MessageSourceWithGroupId

// Not a production SourceBuilder, so not declared as a Component
class BatchSourceBuilder : PipelineSourceBuilder<BatchItemsSourceSpec> {
   override val sourceType: PipelineSourceType = PipelineSourceType.Batch

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is BatchItemsSourceSpec
   }

   override fun buildBatch(
      pipelineSpec: PipelineSpec<BatchItemsSourceSpec, *>,
      inputType: Type?
   ): BatchSource<MessageContentProvider> {
      return SourceBuilder
         .batch("scheduled-source") {}
         .fillBufferFn { _, buf: SourceBuilder.SourceBuffer<MessageContentProvider> ->
            pipelineSpec.input.items.map {
               StringContentProvider(
                  it,
                  pipelineSpec.input.groupId?.let { groupId -> BatchMessageSourceMetadata(groupId) })
            }
               .forEach { buf.add(it) }
            buf.close()
         }
         .build()
   }

   override fun getEmittedType(pipelineSpec: PipelineSpec<BatchItemsSourceSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.typeName
   }

}
