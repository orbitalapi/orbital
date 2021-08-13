package io.vyne.pipelines.runner.transport.cask

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.runner.transport.PipelineTestUtils
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportOutputSpec
import io.vyne.schemas.fqn
import org.junit.Test

class KafkaTransportSpecTest {
   @Test
   fun `can read and write from json`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult(
         input = KafkaTransportInputSpec(
            "input-topic",
            VersionedTypeReference("com.foo.bar.InputType".fqn()),
            props = mapOf(
               "group.id" to "vyne-pipeline-group",
               "bootstrap.servers" to "kafka:9092  ",
               "heartbeat.interval.ms" to "3000",
               "session.timeout.ms" to "10000",
               "auto.offset.reset" to "earliest"
            )
         ),
         output = KafkaTransportOutputSpec(
            "output-topic",
            targetType = VersionedTypeReference("com.foo.bar.InputType".fqn()),
            props = mapOf(
               "group.id" to "vyne-pipeline-group",
               "bootstrap.servers" to "kafka:9092",
               "heartbeat.interval.ms" to "3000",
               "session.timeout.ms" to "10000",
               "auto.offset.reset" to "earliest"
            )
         )
      )
   }
}
