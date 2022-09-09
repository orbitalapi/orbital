package io.vyne.pipelines.jet.source.fixed

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type

/**
 * A pipeline batch source with fixed items.
 * Intended for tests only.
 */
data class BatchItemsSourceSpec(
   val items: List<String>,
   val typeName: QualifiedName,
) : PipelineTransportSpec {
   override val type: PipelineTransportType = "Batch"
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val description: String = "Batch input"
}

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
            pipelineSpec.input.items.map { StringContentProvider(it) }.forEach { buf.add(it) }
            buf.close()
         }
         .build()
   }

   override fun getEmittedType(pipelineSpec: PipelineSpec<BatchItemsSourceSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.typeName
   }

}
