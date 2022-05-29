package io.vyne.pipelines.jet.source.fixed

import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.SourceBuilder.TimestampedSourceBuffer
import com.hazelcast.jet.pipeline.StreamSource
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import java.time.Instant
import java.util.*

/**
 * A pipeline source with only fixed items.
 * Intended for tests only.
 */
data class FixedItemsSourceSpec(
   val items: Queue<String>,
   val typeName: QualifiedName
) : PipelineTransportSpec {
   override val type: PipelineTransportType = "Flux"
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val props: Map<String, Any> = emptyMap()
   override val description: String = "Flux input"
}

// Not a production SourceBuilder, so not declared as a Component
class FixedItemsSourceBuilder : PipelineSourceBuilder<FixedItemsSourceSpec> {
   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is FixedItemsSourceSpec
   }

   override fun build(
      pipelineSpec: PipelineSpec<FixedItemsSourceSpec, *>,
      inputType: Type?
   ): StreamSource<MessageContentProvider> {
      return SourceBuilder
         .timestampedStream("flux-source") { pipelineSpec.input.items }
         .fillBufferFn { obj: Queue<String>, buf: TimestampedSourceBuffer<MessageContentProvider> ->
            if (obj.isNotEmpty()) {
               buf.add(StringContentProvider(obj.poll()), Instant.now().toEpochMilli())
            }
         }
         .build()
   }

   override fun getEmittedType(pipelineSpec: PipelineSpec<FixedItemsSourceSpec, *>, schema:Schema): QualifiedName {
      return pipelineSpec.input.typeName
   }

}
