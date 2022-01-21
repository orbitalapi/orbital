package io.vyne.pipelines.jet.sink.kafka

import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.builders.PostgresJdbcUrlBuilder
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.jet.source.kafka.AbstractKafkaJetTest
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import io.vyne.schemas.fqn
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class KafkaSinkTest : AbstractKafkaJetTest() {


   @Test
   fun canOutputToKafka() {
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


      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         output = KafkaTransportOutputSpec(
            topicName,
            producerProps(),
            "Target"
         )
      )
      val (pipeline, job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)

      val received = consumeMessages(1)
      received.single().should.equal("""{"givenName":"jimmy"}""")
   }
}
