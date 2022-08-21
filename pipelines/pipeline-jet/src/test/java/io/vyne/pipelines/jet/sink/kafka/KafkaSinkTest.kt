package io.vyne.pipelines.jet.sink.kafka

import com.winterbe.expekt.should
import io.vyne.connectors.kafka.KafkaConnectionConfiguration
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.jet.source.kafka.AbstractKafkaJetTest
import io.vyne.schemas.fqn
import io.vyne.utils.Ids
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class KafkaSinkTest : AbstractKafkaJetTest() {


   @Test
   fun canOutputToKafka() {
      // Pipeline Kafka -> Direct
      val (jetInstance, _, vyneProvider) = jetWithSpringAndVyne(
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
      startPipeline(jetInstance, vyneProvider, pipelineSpec)

      val received = consumeMessages(1)
      received.single().should.equal("""{"givenName":"jimmy"}""")
   }
}
