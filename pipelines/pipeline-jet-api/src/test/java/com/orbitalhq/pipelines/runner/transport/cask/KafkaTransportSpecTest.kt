package com.orbitalhq.pipelines.runner.transport.cask

import com.orbitalhq.VersionedTypeReference
import com.orbitalhq.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import com.orbitalhq.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import com.orbitalhq.pipelines.runner.transport.PipelineTestUtils
import com.orbitalhq.schemas.fqn
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
