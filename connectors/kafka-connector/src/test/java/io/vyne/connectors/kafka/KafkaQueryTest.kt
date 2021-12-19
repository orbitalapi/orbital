package io.vyne.connectors.kafka

import com.winterbe.expekt.should
import io.vyne.connectors.kafka.builders.KafkaConnectionBuilder
import io.vyne.connectors.kafka.registry.InMemoryKafkaConfigFileConnectorRegistry
import io.vyne.models.TypedObject
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.testVyne
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.*

@SpringBootTest(classes = [KafkaQueryTestConfig::class])
@RunWith(SpringRunner::class)
@Testcontainers
class KafkaQueryTest {

   val hostName = "kafka"
   lateinit var kafkaProducer:Producer<String,String>

   lateinit var connectionRegistry: InMemoryKafkaConfigFileConnectorRegistry

   @Rule
   @JvmField
   final val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.2"))
      .withNetworkAliases(hostName)

   @Before
   fun before() {
      kafkaContainer.start()
      kafkaContainer.waitingFor(Wait.forListeningPort())

      val props = Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
      props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducer-${Instant.now().toEpochMilli()}")
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.getName())
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.getName())
      props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000)
      kafkaProducer = KafkaProducer(props)

      connectionRegistry = InMemoryKafkaConfigFileConnectorRegistry()

      val connection = DefaultKafkaConnectionConfiguration.forParams(
         "moviesConnection",
         connectionParameters = mapOf(
            KafkaConnectionBuilder.Parameters.BROKERS to kafkaContainer.bootstrapServers,
            KafkaConnectionBuilder.Parameters.TOPIC to "movies",
            KafkaConnectionBuilder.Parameters.OFFSET to "earliest"
         )
      )

      connectionRegistry.register(connection)

   }

   @After
   fun after() {
      kafkaContainer.stop()
   }

   @Test
   fun `can use a TaxiQL statement to consume kafka stream`(): Unit = runBlocking {

      val vyne = testVyne(
         listOf(
            KafkaConnectorTaxi.schema,
            """
         ${KafkaConnectorTaxi.Annotations.imports}
         type MovieId inherits Int
         type MovieTitle inherits String

         model Movie {
            id : MovieId
            title : MovieTitle
         }

         @KafkaService( connectionName = "moviesConnection" )
         service MovieService {
            operation streamMovieQuery():Stream<Movie>
         }

      """
         )
      ) { schema -> listOf(KafkaInvoker(connectionRegistry,SimpleSchemaProvider(schema))) }

      val message1 = "{\"id\": \"1234\",\"title\": \"Title 1\"}"
      val message2 = "{\"id\": \"5678\",\"title\": \"Title 2\"}"

      kafkaProducer.send(ProducerRecord("movies", UUID.randomUUID().toString(), message1))
      kafkaProducer.send(ProducerRecord("movies", UUID.randomUUID().toString(), message2))

      val result = vyne.query("""stream { Movie }""")
         .results.take(2).toList() as List<TypedObject>

      result.should.have.size(2)

   }
}

@Configuration
@EnableAutoConfiguration
class KafkaQueryTestConfig

