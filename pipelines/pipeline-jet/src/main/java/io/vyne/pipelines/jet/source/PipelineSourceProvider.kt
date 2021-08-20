package io.vyne.pipelines.jet.source

import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceBuilder
import io.vyne.pipelines.jet.source.http.poll.PollingTaxiOperationSourceBuilder

class PipelineSourceProvider(
   private val builders: List<PipelineSourceBuilder<*>>
) {

   companion object {
      private val DEFAULT_BUILDERS = listOf<PipelineSourceBuilder<*>>(
         FixedItemsSourceBuilder(),
         PollingTaxiOperationSourceBuilder()
      )
      fun default(): PipelineSourceProvider {
         return PipelineSourceProvider(DEFAULT_BUILDERS)
      }
   }

   fun <I : PipelineTransportSpec> getPipelineSource(pipelineSpec: PipelineSpec<I,*>): PipelineSourceBuilder<I> {
      return builders.firstOrNull { it.canSupport(pipelineSpec) } as PipelineSourceBuilder<I>?
         ?: error("No pipeline builder exists for spec of type ${pipelineSpec.input::class.simpleName}")
   }
}
