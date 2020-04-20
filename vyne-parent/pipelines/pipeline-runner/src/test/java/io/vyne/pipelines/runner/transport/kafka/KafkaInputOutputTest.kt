package io.vyne.pipelines.runner.transport.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.query.VyneJacksonModule
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.junit.Test

class KafkaInputOutputTest : AbstractKafkaTest() {
   @Test
   fun canSerializePipeline() {
      val inputTopicName = "pipeline-input"
      val outputTopicName = "pipeline-output"
      val pipeline = Pipeline(
         input = PipelineChannel(
            VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            KafkaTransportInputSpec(
               topic = inputTopicName,
               targetType = VersionedTypeReference("PersonLoggedOnEvent".fqn()),
               props = consumerProps("vyne-pipeline-group")
            )
         ),
         output = PipelineChannel(
            VersionedTypeReference("UserEvent".fqn()),
            KafkaTransportOutputSpec(
               topic = outputTopicName,
               targetType = VersionedTypeReference("UserEvent".fqn()),
               props = producerProps()
            )
         ),
         name = "test-pipeline"
      )

      val json = jacksonObjectMapper()
         .registerModule(VyneJacksonModule())
         .writerWithDefaultPrettyPrinter().writeValueAsString(pipeline)
      log().info("PipelineJson: \n$json")
   }
}
