package io.vyne.pipelines.jet.source

import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceBuilder
import io.vyne.pipelines.jet.source.fixed.ItemStreamSourceBuilder
import io.vyne.pipelines.jet.source.http.poll.PollingTaxiOperationSourceBuilder
import io.vyne.pipelines.jet.source.kafka.KafkaSourceBuilder

class PipelineSourceProvider(
   private val builders: List<PipelineSourceBuilder<*>>
) {

   companion object {
      private val DEFAULT_BUILDERS = listOf<PipelineSourceBuilder<*>>(
         FixedItemsSourceBuilder(),
         ItemStreamSourceBuilder(),
         PollingTaxiOperationSourceBuilder(),
         KafkaSourceBuilder() // TODO : This should be spring-wired, to inject the config
      )

      fun default(): PipelineSourceProvider {
         return PipelineSourceProvider(DEFAULT_BUILDERS)
      }
   }

   fun <I : PipelineTransportSpec> getPipelineSource(pipelineSpec: PipelineSpec<I, *>): PipelineSourceBuilder<I> {
      return builders.firstOrNull { it.canSupport(pipelineSpec) } as PipelineSourceBuilder<I>?
         ?: error("No pipeline builder exists for spec of type ${pipelineSpec.input::class.simpleName}")
   }
}
