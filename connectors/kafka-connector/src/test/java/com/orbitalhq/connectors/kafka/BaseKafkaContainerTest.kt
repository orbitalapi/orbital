package com.orbitalhq.connectors.kafka

import com.orbitalhq.Vyne
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.InMemoryKafkaConnectorRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryResult
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random

@Testcontainers
abstract class BaseKafkaContainerTest {
   val hostName = "kafka"

   private val logger = KotlinLogging.logger {}

   lateinit var kafkaProducer: Producer<String, ByteArray>
   lateinit var connectionRegistry: InMemoryKafkaConnectorRegistry

   @Rule
   @JvmField
   final val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.2"))
      .withStartupTimeout(Duration.ofMinutes(2))
      .withNetworkAliases(hostName)

   @Before
   open fun before() {
      kafkaContainer.start()
      kafkaContainer.waitingFor(Wait.forListeningPort())
   }

   @After
   fun after() {
      kafkaContainer.stop()
   }

   fun buildProducer(connectionName: String = "moviesConnection"): Pair<KafkaProducer<String, ByteArray>, InMemoryKafkaConnectorRegistry> {
      val props = Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
      props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducer-${Instant.now().toEpochMilli()}")
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java.name)
      props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000)
      val kafkaProducer = KafkaProducer<String,ByteArray>(props)

      val connectionRegistry = InMemoryKafkaConnectorRegistry()

      val connection = KafkaConnectionConfiguration(
         connectionName,
         kafkaContainer.bootstrapServers,
         "VyneTest-" + Random.nextInt(),
      )

      connectionRegistry.register(connection)
      this.kafkaProducer = kafkaProducer
      this.connectionRegistry = connectionRegistry

      return kafkaProducer to connectionRegistry
   }

   fun sendMessage(message: ByteArray, topic: String = "movies"): RecordMetadata {
      logger.info { "Sending message to topic $topic" }
      val metadata = kafkaProducer.send(ProducerRecord(topic, UUID.randomUUID().toString(), message))
         .get()
      logger.info { "message sent to topic $topic with offset ${metadata.offset()}" }
      return metadata
   }

   fun sendMessage(message: String, topic: String = "movies"): RecordMetadata {
      return sendMessage(message.toByteArray(), topic)
   }

   fun vyneWithKafkaInvoker(taxi: String): Pair<Vyne, KafkaStreamManager> {
      val schema = TaxiSchema.fromStrings(
         listOf(
            KafkaConnectorTaxi.schema,
            taxi
         )
      )
      val kafkaStreamManager = KafkaStreamManager(connectionRegistry, SimpleSchemaProvider(schema))
      val invokers = listOf(
         KafkaInvoker(kafkaStreamManager)
      )
      return testVyne(schema, invokers) to kafkaStreamManager
   }

   fun collectQueryResults(query: QueryResult, resultsFromQuery1: MutableList<TypedInstance>) {
      GlobalScope.async {
         logger.info { "Collecting..." }
         query.results
            .collect {
               resultsFromQuery1.add(it)
               logger.info { "received event - have now captured ${resultsFromQuery1.size} events in result handler" }
            }
      }
   }

}
