package io.vyne.pipelines.jet.source

import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.source.aws.s3.S3SourceBuilder
import io.vyne.pipelines.jet.source.aws.sqss3.SqsS3SourceBuilder
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceBuilder
import io.vyne.pipelines.jet.source.fixed.ItemStreamSourceBuilder
import io.vyne.pipelines.jet.source.http.poll.PollingTaxiOperationSourceBuilder
import io.vyne.pipelines.jet.source.kafka.KafkaSourceBuilder

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
               FixedItemsSourceBuilder(),
               ItemStreamSourceBuilder(),
               PollingTaxiOperationSourceBuilder(),
               S3SourceBuilder(),
               SqsS3SourceBuilder(),
               KafkaSourceBuilder(kafkaConnectionRegistry)
            )
         )
      }
   }

   fun <I : PipelineTransportSpec> getPipelineSource(pipelineSpec: PipelineSpec<I, *>): PipelineSourceBuilder<I> {
      return builders.firstOrNull { it.canSupport(pipelineSpec) } as PipelineSourceBuilder<I>?
         ?: error("No pipeline builder exists for spec of type ${pipelineSpec.input::class.simpleName}")
   }
}
