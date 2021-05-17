package io.vyne.pipelines.runner.transport.kafka

import io.vyne.VersionedTypeReference
import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.PipelineBuilder
//import io.vyne.pipelines.runner.PipelineTestUtils
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils

//
//@EmbeddedKafka(partitions = 1)
//open class AbstractKafkaTest {
//
//   @JvmField
//   @Rule
//   val testName = TestName()
//   protected lateinit var topicName: String
//
//   @Autowired
//   protected lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker
//
//   @Before
//   fun setup() {
//      topicName = testName.methodName
//      embeddedKafkaBroker.addTopics(topicName)
//   }
//
//   fun <T> sendKafkaMessage(message: T) {
//      val record = ProducerRecord<String, T>(topicName, message)
//      val producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker)
//      val producer = KafkaProducer<String, T>(producerProps)
//      producer.send(record)
//   }
//
//   fun consumerProps(): Map<String, Any> {
//      val consumerProps = KafkaTestUtils.consumerProps("vyne-pipeline-group", "false", embeddedKafkaBroker);
//
//      val props = HashMap<String, String>()
//      props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = "3000"
//      props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = "60000"
//      props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
//      return consumerProps + props
//   }
//
//   fun kafkaInputSpec() = KafkaTransportInputSpec(
//      topic = topicName,
//      targetType = VersionedTypeReference("PersonLoggedOnEvent".fqn()),
//      props = consumerProps()
//   )
//
//   fun directOutputSpec(name:String = "Unnamed") = DirectOutputSpec(name)
//
//
//   fun buildPipelineBuilder(): PipelineBuilder {
//      val (vyne, stub) = PipelineTestUtils.pipelineTestVyne()
//      stub.addResponse("getUserNameFromId") { _, params ->
//         vyne.parseKeyValuePair("Username", params[0].second.value as String + "@mail.com")
//      }
//
//      return PipelineBuilder(
//         PipelineTransportFactory(listOf(KafkaInputBuilder(), DirectOutputBuilder())),
//         SimpleVyneProvider(vyne),
//         ObserverProvider.local()
//      )
//   }
//
//   fun buildPipeline(inputTransportSpec: PipelineTransportSpec, outputTransportSpec: PipelineTransportSpec): Pipeline {
//
//      return Pipeline(
//         "testPipeline",
//         input = PipelineChannel(VersionedTypeReference("PersonLoggedOnEvent".fqn()), inputTransportSpec),
//         output = PipelineChannel(VersionedTypeReference("UserEvent".fqn()), outputTransportSpec)
//      )
//
//   }
//
//}
