package com.orbitalhq.pipelines.jet.sink.kafka

import com.winterbe.expekt.should
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import com.orbitalhq.pipelines.jet.queueOf
import com.orbitalhq.pipelines.jet.source.fixed.FixedItemsSourceSpec
import com.orbitalhq.pipelines.jet.source.kafka.AbstractKafkaJetTest
import com.orbitalhq.schemas.fqn
import com.orbitalhq.utils.Ids
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class KafkaSinkTest : AbstractKafkaJetTest() {


   @Test
   fun canOutputToKafka() {
      // Pipeline Kafka -> Direct
      val (hazelcastInstance, _, vyneClient) = jetWithSpringAndVyne(
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

      kafkaConnectionRegistry.register(
         KafkaConnectionConfiguration(
            connectionName = "my-kafka-connection",
            brokerAddress = kafkaContainer.bootstrapServers,
            groupId = Ids.id("kafka-test")
         )
      )
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(
            KafkaTransportOutputSpec(
               "my-kafka-connection",
               topicName,
               "Target"
            )
         )
      )
      startPipeline(hazelcastInstance, vyneClient, pipelineSpec)

      val received = consumeMessages(1)
      received.single().should.equal("""{"givenName":"jimmy"}""")
   }
}
