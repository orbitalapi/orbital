package io.vyne.pipelines.jet.sink

import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.sink.http.TaxiOperationSinkBuilder
import io.vyne.pipelines.jet.sink.kafka.KafkaSinkBuilder
import io.vyne.pipelines.jet.sink.list.ListSinkBuilder
import io.vyne.pipelines.jet.sink.jdbc.JdbcSinkBuilder
import io.vyne.pipelines.jet.sink.redshift.RedshiftSinkBuilder

class PipelineSinkProvider(
   private val builders: List<PipelineSinkBuilder<*, *>>
) {


   fun <O : PipelineTransportSpec> getPipelineSink(pipelineSpec: PipelineSpec<*, O>): PipelineSinkBuilder<O, Any> {
      return builders.firstOrNull { it.canSupport(pipelineSpec) } as PipelineSinkBuilder<O, Any>?
         ?: error("No sink builder exists for spec of type ${pipelineSpec.output::class.simpleName}")
   }

   companion object {
      /**
       * Used in testing. Use spring in app runtime.
       * Wires up the standard source providers.
       * To avoid a test-time dependency on Spring, takes the
       * required dependencies as parameters
       *
       */
      fun default(
         kafkaConnectionRegistry: KafkaConnectionRegistry
      ): PipelineSinkProvider {
         return PipelineSinkProvider(
            listOf(
               ListSinkBuilder(),
               TaxiOperationSinkBuilder(),
               KafkaSinkBuilder(kafkaConnectionRegistry),
               RedshiftSinkBuilder(), // TODO : This should be spring-wired, to inject the config.
               JdbcSinkBuilder()
            )
         )
      }
   }

}
