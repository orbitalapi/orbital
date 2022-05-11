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
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
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
         output = outputSpec
      )
      val (pipeline, job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)

      // Send for messages into kafka
      sendKafkaMessage(""" {"firstName":"Jimmy", "lastName" : "Schmitt" } """)
      sendKafkaMessage(""" {"firstName":"Jack", "lastName" : "Jones" } """)
      sendKafkaMessage(""" {"firstName":"Paul", "lastName" : "Pratt" } """)

      await().atMost(Duration.ofSeconds(10)).until {
         listSinkTarget.size == 3
      }

   }
//
//   @Test
//   fun `corruptMessageIgnored`() {
//      // Pipeline CustomKafka -> Direct
//      val pipeline = buildPipeline(
//         inputTransportSpec = customKafkaTransportInputSpec(),
//         outputTransportSpec = directOutputSpec()
//      )
//      val pipelineInstance = buildPipelineBuilder().build(pipeline)
//      pipelineInstance.output.healthMonitor.reportStatus(UP)
//
//      // Send for messages into kafka
//      sendKafkaMessage(""" {"userId":"Marty"} """)
//      sendKafkaMessage(""" {"userId":"Paul"} """)
//      // This will throw in BankKafkaInput::getBody
//      sendKafkaMessage(""" {"userId":"Serhat"} """)
//      sendKafkaMessage(""" {"userId":"Markus"} """)
//
//      // Wait until we wrote 4 messages in the output
//      val output = pipelineInstance.output as DirectOutput
//      await().until { output.messages.should.have.size(3) }
//
//      // Check the values in the output
//      output.messages.should.have.all.elements(
//         """{"id":"Marty","name":"Marty@mail.com"}""",
//         """{"id":"Paul","name":"Paul@mail.com"}""",
//         """{"id":"Markus","name":"Markus@mail.com"}"""
//      )
//
//   }

}
