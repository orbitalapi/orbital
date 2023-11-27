package com.orbitalhq.pipelines.jet.sink

import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.sink.aws.s3.AwsS3SinkBuilder
import com.orbitalhq.pipelines.jet.sink.http.TaxiOperationSinkBuilder
import com.orbitalhq.pipelines.jet.sink.jdbc.JdbcSinkBuilder
import com.orbitalhq.pipelines.jet.sink.kafka.KafkaSinkBuilder
import com.orbitalhq.pipelines.jet.sink.list.ListSinkBuilder
import com.orbitalhq.pipelines.jet.sink.log.LoggingSinkBuilder
import com.orbitalhq.pipelines.jet.sink.redshift.RedshiftSinkBuilder
import com.orbitalhq.pipelines.jet.sink.stream.StreamSinkBuilder

class PipelineSinkProvider(
   private val builders: List<PipelineSinkBuilder<*, *>>
) {


   fun <O : PipelineTransportSpec> getPipelineSink(pipelineTransportSpec: O): PipelineSinkBuilder<O, Any> {
      return builders.firstOrNull { it.canSupport(pipelineTransportSpec) } as PipelineSinkBuilder<O, Any>?
         ?: error("No sink builder exists for spec of type ${pipelineTransportSpec::class.simpleName}")
   }

   companion object {
      /**
       * Used in testing. Use spring in app runtime.
       * Wires up the standard source providers.
       * To avoid a test-time dependency on Spring, takes the
       * required dependencies as parameters.
       *
       */
      fun default(
         kafkaConnectionRegistry: KafkaConnectionRegistry,
         awsConnectionRegistry: AwsConnectionRegistry
      ): PipelineSinkProvider {
         // TODO : This should be spring-wired, to inject the config.
         return PipelineSinkProvider(
            listOf(
               StreamSinkBuilder(),
               ListSinkBuilder(),
               TaxiOperationSinkBuilder(),
               KafkaSinkBuilder(kafkaConnectionRegistry),
               RedshiftSinkBuilder(),
               JdbcSinkBuilder(),
               AwsS3SinkBuilder(awsConnectionRegistry),
               LoggingSinkBuilder()
            )
         )
      }
   }

}
