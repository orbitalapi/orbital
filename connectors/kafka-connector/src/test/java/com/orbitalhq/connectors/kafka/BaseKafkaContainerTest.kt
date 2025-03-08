package com.orbitalhq.connectors.kafka

import com.orbitalhq.errors.ErrorType
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.Vyne
import com.orbitalhq.avro.AvroFormatSpec
import com.orbitalhq.connectors.StreamErrorPublisher
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.InMemoryKafkaConnectorRegistry
import com.orbitalhq.metrics.GaugeRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.DefaultFormatRegistry
import com.orbitalhq.protobuf.ProtobufFormatSpec
import com.orbitalhq.query.QueryResult
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyneWithStub
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.Header
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
import java.util.Collections.singleton
import java.util.Properties
import java.util.UUID
import kotlin.random.Random

@Testcontainers
abstract class BaseKafkaContainerTest {
   val hostName = "kafka"

   private val logger = KotlinLogging.logger {}

   lateinit var kafkaProducer: Producer<String, ByteArray>
   lateinit var connectionRegistry: InMemoryKafkaConnectorRegistry

   val formatRegistry = DefaultFormatRegistry(listOf(ProtobufFormatSpec, AvroFormatSpec))

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
      val kafkaProducer = KafkaProducer<String, ByteArray>(props)

      val connectionRegistry = InMemoryKafkaConnectorRegistry()

      val connection = KafkaConnectionConfiguration(
         connectionName,
         kafkaContainer.bootstrapServers,
         "VyneTest-" + Random.nextInt(),
      )

      val invalidConnection = KafkaConnectionConfiguration(
         "invalidConnection",
         kafkaContainer.bootstrapServers,
         "VyneTest-" + Random.nextInt(),
         mapOf("security.protocol" to "SASL_PLAINTEXT",
            "sasl.mechanism" to "PLAIN",
            "sasl.jaas.config" to "org.apache.kafka.common.security.plain.PlainLoginModule required username='myKafkaUser' password='asdas'")
      )

      connectionRegistry.register(connection)
      connectionRegistry.register(invalidConnection)
      this.kafkaProducer = kafkaProducer
      this.connectionRegistry = connectionRegistry

      return kafkaProducer to connectionRegistry
   }

   fun createTopic(topic: String, numPartitions: Int = 1) {
      val adminProperties = Properties();
      adminProperties[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaContainer.bootstrapServers
      val admin = Admin.create(adminProperties)
      val newTopic = NewTopic(topic, numPartitions, 1)
      admin.createTopics(singleton(newTopic))
   }

   fun sendMessage(message: ByteArray, topic: String = "movies", key:String = UUID.randomUUID().toString(), headers: List<Header> = emptyList()): RecordMetadata {
      logger.info { "Sending message to topic $topic" }
      val metadata = kafkaProducer.send(ProducerRecord(
         topic,
         null, // partition
         key,
         message,
         headers
      ))
         .get()
      logger.info { "message sent to topic $topic with offset ${metadata.offset()}" }
      return metadata
   }

   fun sendMessage(message: String, topic: String = "movies"): RecordMetadata {
      return sendMessage(message.toByteArray(), topic)
   }

   fun vyneWithKafkaInvoker(sourcePackage: SourcePackage): KafkaTestSetUp {
      val schema = TaxiSchema.from(
         listOf(
            sourcePackage,
            SourcePackage(
               PackageMetadata.from(PackageIdentifier.fromId("com.orbitalhq/kafka/1.0.0")),
               listOf(VersionedSource.sourceOnly(KafkaConnectorTaxi.schema))
            )
         )
      )
      return vyneWithKafkaInvoker(schema)
   }

   fun vyneWithKafkaInvoker(taxi: String): KafkaTestSetUp {
      val schema = TaxiSchema.fromStrings(
         listOf(
            KafkaConnectorTaxi.schema,
            ErrorType.queryErrorVersionedSource.content,
            taxi
         )
      )
      return vyneWithKafkaInvoker(schema)
   }

   fun vyneWithKafkaInvoker(schema: TaxiSchema): KafkaTestSetUp {
      val kafkaStreamPublisher = KafkaStreamPublisher(
         connectionRegistry,
         formatRegistry = formatRegistry,
         meterRegistry = SimpleMeterRegistry()
      )
      val kafkaStreamManager = KafkaStreamManager(
         connectionRegistry,
         SimpleSchemaProvider(schema),
         formatRegistry = formatRegistry,
         meterRegistry = SimpleMeterRegistry(),
         emitConsumerInfoMessages = false,
         kafkaConsumerStatsFlowBuilder = KafkaConsumerStatsFlowBuilder(GaugeRegistry.simple())
      )
      val streamErrorPublisher = StreamErrorPublisher()
      val invokers = listOf(
         KafkaInvoker(kafkaStreamManager, kafkaStreamPublisher, streamErrorPublisher),
      )
      val (vyne, stub) = testVyneWithStub(schema, invokers)
      return KafkaTestSetUp(vyne, kafkaStreamManager, stub, streamErrorPublisher)
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

data class KafkaTestSetUp(
   val vyne: Vyne,
   val kafkaStreamManager: KafkaStreamManager,
   val stubService: StubService,
   val streamErrorPublisher: StreamErrorPublisher
)
