package com.orbitalhq.pipelines.jet.source

import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.source.aws.s3.S3SourceBuilder
import com.orbitalhq.pipelines.jet.source.aws.sqss3.SqsS3SourceBuilder
import com.orbitalhq.pipelines.jet.source.file.FileWatcherStreamSourceBuilder
import com.orbitalhq.pipelines.jet.source.fixed.BatchSourceBuilder
import com.orbitalhq.pipelines.jet.source.fixed.FixedItemsSourceBuilder
import com.orbitalhq.pipelines.jet.source.fixed.ItemStreamSourceBuilder
import com.orbitalhq.pipelines.jet.source.fixed.ScheduledSourceBuilder
import com.orbitalhq.pipelines.jet.source.http.poll.PollingTaxiOperationSourceBuilder
import com.orbitalhq.pipelines.jet.source.kafka.KafkaSourceBuilder
import com.orbitalhq.pipelines.jet.source.query.PollingQuerySourceBuilder

class PipelineSourceProvider(
   private val builders: List<PipelineSourceBuilder<*>>
) {

   companion object {

      /**
       * Used in testing.  Use spring in app-runtime.
       * Wires up the standard source providers.
       * To avoid a test-time dependency on Spring, takes the
       * required dependencies as parameters
       *
       */
      fun default(
         kafkaConnectionRegistry: KafkaConnectionRegistry
      ): PipelineSourceProvider {

         return PipelineSourceProvider(
            listOf(
               ScheduledSourceBuilder(),
               FixedItemsSourceBuilder(),
               BatchSourceBuilder(),
               ItemStreamSourceBuilder(),
               PollingTaxiOperationSourceBuilder(),
               S3SourceBuilder(),
               SqsS3SourceBuilder(),
               KafkaSourceBuilder(kafkaConnectionRegistry),
               PollingQuerySourceBuilder(),
               FileWatcherStreamSourceBuilder()
            )
         )
      }
   }

   fun <I : PipelineTransportSpec> getPipelineSource(pipelineSpec: PipelineSpec<I, *>): PipelineSourceBuilder<I> {
      return builders.firstOrNull { it.canSupport(pipelineSpec) } as PipelineSourceBuilder<I>?
         ?: error("No pipeline builder exists for spec of type ${pipelineSpec.input::class.simpleName}")
   }
}
