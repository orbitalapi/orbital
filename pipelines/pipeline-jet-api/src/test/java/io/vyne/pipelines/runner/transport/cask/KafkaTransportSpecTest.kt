package io.vyne.pipelines.runner.transport.cask

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import io.vyne.pipelines.runner.transport.PipelineTestUtils
import io.vyne.schemas.fqn
import org.junit.Test

class KafkaTransportSpecTest {
   @Test
   fun `can read and write from json`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult(
         input = KafkaTransportInputSpec(
            connectionName = "my-kafka-connection",
            topic = "input-topic",
            targetTypeName = "com.foo.bar.InputType"
         ),
         output = KafkaTransportOutputSpec(
            connectionName = "output-topic",
            topic = "output-topic",
            targetTypeName = "com.foo.bar.OutputType"
         )
      )
   }
}
