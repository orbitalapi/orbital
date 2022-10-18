package io.vyne.pipelines.jet.source.fixed

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import io.vyne.pipelines.jet.api.transport.CronExpression
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.ScheduledPipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import java.util.*

/**
 * A scheduled pipeline source with fixed items.
 * Intended for tests only.
 */
data class ScheduledSourceSpec(
   val items: Queue<String>,
   val typeName: QualifiedName,
   override val pollSchedule: CronExpression = CronExpressions.EVERY_SECOND,
   override val preventConcurrentExecution: Boolean = false
) : ScheduledPipelineTransportSpec {
   override val type: PipelineTransportType = "Scheduled"
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val description: String = "Scheduled input"
}

// Not a production SourceBuilder, so not declared as a Component
class ScheduledSourceBuilder : PipelineSourceBuilder<ScheduledSourceSpec> {
   override val sourceType: PipelineSourceType = PipelineSourceType.Batch

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is ScheduledSourceSpec
   }

   override fun buildBatch(
      pipelineSpec: PipelineSpec<ScheduledSourceSpec, *>,
      inputType: Type?
   ): BatchSource<MessageContentProvider> {
      return SourceBuilder
         .batch("scheduled-source") {}
         .fillBufferFn { _, buf: SourceBuilder.SourceBuffer<MessageContentProvider> ->
            pipelineSpec.input.items.map { StringContentProvider(it) }.forEach { buf.add(it) }
         }
         .build()
   }

   override fun getEmittedType(pipelineSpec: PipelineSpec<ScheduledSourceSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.typeName
   }

}
