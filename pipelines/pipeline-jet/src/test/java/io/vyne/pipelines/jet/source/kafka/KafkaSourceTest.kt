package io.vyne.pipelines.jet.source.kafka

import io.vyne.connectors.kafka.KafkaConnectionConfiguration
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import io.vyne.utils.Ids
import org.awaitility.Awaitility.await
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import java.time.Duration

@RunWith(SpringRunner::class)
class KafkaSourceTest : AbstractKafkaJetTest() {

   @Test
   @Ignore("failing to connect to kafka in a test, needs investigation")
   fun canReceiveStringFromKakfaInput() {
      // Pipeline Kafka -> Direct
      val (hazelcastInstance, applicationContext, vyneClient) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """, emptyList()
      )

      // Register kafka connector
      kafkaConnectionRegistry.register(
         KafkaConnectionConfiguration(
            "my-kafka-connection", kafkaContainer.bootstrapServers, Ids.id("kafka-integration-test")
         )
      )

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = KafkaTransportInputSpec(
            "my-kafka-connection",
            topicName,
            "Person"
         ),
         outputs = listOf(outputSpec)
      )
      startPipeline(hazelcastInstance, vyneClient, pipelineSpec)

      // Send for messages into kafka
      sendKafkaMessage(""" {"firstName":"Jimmy", "lastName" : "Schmitt" } """)
      sendKafkaMessage(""" {"firstName":"Jack", "lastName" : "Jones" } """)
      sendKafkaMessage(""" {"firstName":"Paul", "lastName" : "Pratt" } """)

      await().atMost(Duration.ofSeconds(10)).until {
         listSinkTarget.size == 3
      }

   }
}
