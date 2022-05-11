package io.vyne.pipelines.jet.source.kafka

import com.winterbe.expekt.should
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.utils.log
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@Testcontainers
open class AbstractKafkaJetTest : BaseJetIntegrationTest() {

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
   }

   fun <T> sendKafkaMessage(message: T) {
      val record = ProducerRecord<String, T>(topicName, message)
      val producerProps = KafkaTestUtils.senderProps(kafkaContainer.bootstrapServers)
      val producer = KafkaProducer<String, T>(producerProps)
      val sentRecord = producer.send(record).get()
      log().info("Message sent to topic $topicName with offset ${sentRecord.offset()}")
   }

   fun producerProps():Map<String,Any> {
      val producerProps = KafkaTestUtils.senderProps(kafkaContainer.bootstrapServers)
      producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName!!
      producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName!!
      return producerProps
   }
   fun consumerProps(): Map<String, Any> {
      val consumerProps = KafkaTestUtils.consumerProps(kafkaContainer.bootstrapServers,"vyne-pipeline-group", "false")

      val props = HashMap<String, String>()
      props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = "3000"
      props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = "60000"
      // Make sure you set the offset as earliest, because by the
      // time consumer starts, producer might have sent all messages
      props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
      props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.qualifiedName!!
      props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.qualifiedName!!
      return consumerProps + props
   }

   fun consumeMessages(messageCount:Int, timeout:Duration = Duration.ofSeconds(5)): MutableList<String> {

      val consumerProps: Map<String, Any> = consumerProps()
      val receivedMessages = mutableListOf<String>()
      val latch = CountDownLatch(messageCount)
      val executorService: ExecutorService = Executors.newSingleThreadExecutor()
      executorService.execute {
         val kafkaConsumer: KafkaConsumer<Int, String> = KafkaConsumer(consumerProps)
         kafkaConsumer.subscribe(listOf(topicName))
         kafkaConsumer.use { consumer ->
            while (true) {
               val records: ConsumerRecords<Int, String> = consumer.poll(Duration.ofMillis(100))
               records.iterator().forEachRemaining { record ->
                  receivedMessages.add(record.value())
                  latch.countDown()
               }
            }
         }
      }
      latch.await(timeout.seconds, TimeUnit.SECONDS)
      receivedMessages.size.should.equal(messageCount)
      return receivedMessages
   }

}
