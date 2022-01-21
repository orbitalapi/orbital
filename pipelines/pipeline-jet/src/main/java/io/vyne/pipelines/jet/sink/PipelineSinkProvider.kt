package io.vyne.pipelines.jet.sink

import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.sink.http.TaxiOperationSinkBuilder
import io.vyne.pipelines.jet.sink.kafka.KafkaSinkBuilder
import io.vyne.pipelines.jet.sink.list.ListSinkBuilder
import io.vyne.pipelines.jet.sink.redshift.JdbcSinkBuilder
import io.vyne.pipelines.jet.sink.redshift.RedshiftSinkBuilder

class PipelineSinkProvider(
   private val builders: List<PipelineSinkBuilder<*>>
) {

   fun <O : PipelineTransportSpec> getPipelineSink(pipelineSpec: PipelineSpec<*, O>): PipelineSinkBuilder<O> {
      return builders.firstOrNull { it.canSupport(pipelineSpec) } as PipelineSinkBuilder<O>?
         ?: error("No sink builder exists for spec of type ${pipelineSpec.output::class.simpleName}")
   }

   companion object {
      private val DEFAULT_BUILDERS = listOf<PipelineSinkBuilder<*>>(
         ListSinkBuilder(),
         TaxiOperationSinkBuilder(),
         KafkaSinkBuilder(),  // TODO : This should be spring-wired, to inject the config
         RedshiftSinkBuilder(), // TODO : This should be spring-wired, to inject the config.
         JdbcSinkBuilder()
      )
      fun default(): PipelineSinkProvider {
         return PipelineSinkProvider(DEFAULT_BUILDERS)
      }
   }

}
