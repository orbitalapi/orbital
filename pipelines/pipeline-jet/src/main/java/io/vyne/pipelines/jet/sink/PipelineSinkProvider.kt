package io.vyne.pipelines.jet.sink

import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.jet.sink.http.TaxiOperationSinkBuilder
import io.vyne.pipelines.jet.sink.kafka.KafkaSinkBuilder
import io.vyne.pipelines.jet.sink.list.ListSinkBuilder

class PipelineSinkProvider(
   private val builders: List<PipelineSinkBuilder<*>>
) {

   fun <O : PipelineTransportSpec> getPipelineSink(pipelineSpec: PipelineSpec<*,O>): PipelineSinkBuilder<O> {
      return builders.firstOrNull { it.canSupport(pipelineSpec) } as PipelineSinkBuilder<O>?
         ?: error("No sink builder exists for spec of type ${pipelineSpec.output::class.simpleName}")
   }

   companion object {
      private val DEFAULT_BUILDERS = listOf<PipelineSinkBuilder<*>>(
         ListSinkBuilder(),
         TaxiOperationSinkBuilder(),
         KafkaSinkBuilder()  // TODO : This should be spring-wired, to inject the config
      )
      fun default(): PipelineSinkProvider {
         return PipelineSinkProvider(DEFAULT_BUILDERS)
      }
   }

}
