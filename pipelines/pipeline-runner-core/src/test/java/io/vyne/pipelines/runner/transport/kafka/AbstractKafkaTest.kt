package io.vyne.pipelines.runner.transport.kafka

import io.vyne.VersionedTypeReference
import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.PipelineBuilder
import io.vyne.pipelines.runner.PipelineTestUtils
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.pipelines.runner.transport.direct.DirectOutputBuilder
import io.vyne.pipelines.runner.transport.direct.DirectOutputSpec
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleVyneProvider
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName


@Testcontainers
open class AbstractKafkaTest {

   @JvmField
   @Rule
   val testName = TestName()
   protected lateinit var topicName: String


   @JvmField
   @Rule
   val kafkaContainer =  KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))


   @Before
   fun setup() {
      topicName = testName.methodName
      //embeddedKafkaBroker.addTopics(topicName)
   }

   fun <T> sendKafkaMessage(message: T) {
      val record = ProducerRecord<String, T>(topicName, message)
      val producerProps = KafkaTestUtils.senderProps(kafkaContainer.bootstrapServers)
      val producer = KafkaProducer<String, T>(producerProps)
      producer.send(record)
   }

   private fun consumerProps(): Map<String, Any> {
      val consumerProps = KafkaTestUtils.consumerProps(kafkaContainer.bootstrapServers, "vyne-pipeline-group", "false")

      val props = HashMap<String, String>()
      props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = "3000"
      props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = "60000"
      props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
      return consumerProps + props
   }

   fun kafkaInputSpec() = KafkaTransportInputSpec(
      topic = topicName,
      targetType = VersionedTypeReference("PersonLoggedOnEvent".fqn()),
      props = consumerProps()
   )

   fun customKafkaTransportInputSpec()  = CustomKafkaTransportInputSpec(
      topic = topicName,
      targetType = VersionedTypeReference("PersonLoggedOnEvent".fqn()),
      props = consumerProps()
   )

   fun directOutputSpec(name:String = "Unnamed") = DirectOutputSpec(name)


   fun buildPipelineBuilder(): PipelineBuilder {
      val (vyne, stub) = PipelineTestUtils.pipelineTestVyne()
      stub.addResponse("getUserNameFromId") { _, params ->
         listOf(vyne.parseKeyValuePair("Username", params[0].second.value as String + "@mail.com"))
      }

      return PipelineBuilder(
         PipelineTransportFactory(listOf(KafkaInputBuilder(), DirectOutputBuilder(), CustomKafkaInputBuilder())),
         SimpleVyneProvider(vyne),
         ObserverProvider.local()
      )
   }

   fun buildPipeline(inputTransportSpec: PipelineTransportSpec, outputTransportSpec: PipelineTransportSpec): Pipeline {

      return Pipeline(
         "testPipeline",
         input = PipelineChannel(VersionedTypeReference("PersonLoggedOnEvent".fqn()), inputTransportSpec),
         output = PipelineChannel(VersionedTypeReference("UserEvent".fqn()), outputTransportSpec)
      )

   }

}
