package io.vyne.connectors.kafka

import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.Vyne
import io.vyne.connectors.kafka.registry.InMemoryKafkaConfigFileConnectorRegistry
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemaApi.SimpleSchemaProvider
import io.vyne.testVyne
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
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
import java.time.Duration
import java.time.Instant
import java.util.Properties
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

@SpringBootTest(classes = [KafkaQueryTestConfig::class])
@RunWith(SpringRunner::class)
@Testcontainers
class KafkaQueryTest {

   private val logger = KotlinLogging.logger {}

   val hostName = "kafka"
   lateinit var kafkaProducer: Producer<String, String>

   lateinit var connectionRegistry: InMemoryKafkaConfigFileConnectorRegistry

   @Rule
   @JvmField
   final val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.2"))
      .withStartupTimeout(Duration.ofMinutes(2))
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

      val connection = KafkaConnectionConfiguration(
         "moviesConnection",
         kafkaContainer.bootstrapServers,
         "VyneTest-" + Random.nextInt(),
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
            @KafkaOperation( topic = "movies", offset = "earliest" )
            operation streamMovieQuery():Stream<Movie>
         }

      """
         )
      ) { schema -> listOf(KafkaInvoker(connectionRegistry, SimpleSchemaProvider(schema))) }

      val message1 = "{\"id\": \"1234\",\"title\": \"Title 1\"}"
      val message2 = "{\"id\": \"5678\",\"title\": \"Title 2\"}"

      kafkaProducer.send(ProducerRecord("movies", UUID.randomUUID().toString(), message1))
      kafkaProducer.send(ProducerRecord("movies", UUID.randomUUID().toString(), message2))

      val result = vyne.query("""stream { Movie }""")
         .results.take(2).toList() as List<TypedObject>

      result.should.have.size(2)
   }

   @Test
   fun `can consume from the same topic concurrently`(): Unit = runBlocking {

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
            @KafkaOperation( topic = "movies", offset = "earliest" )
            operation streamMovieQuery():Stream<Movie>
         }

      """
         )
      ) { schema -> listOf(KafkaInvoker(connectionRegistry, SimpleSchemaProvider(schema))) }

      val message = """{ "id": "5678", "title": "Title 2"}"""

      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val future1 = buildQuery(vyne, "query1", resultsFromQuery1)

      val resultsFromQuery2 = mutableListOf<TypedInstance>()
      val future2 = buildQuery(vyne, "query2", resultsFromQuery1)


      kafkaProducer.send(ProducerRecord("movies", UUID.randomUUID().toString(), message))
      kafkaProducer.send(ProducerRecord("movies", UUID.randomUUID().toString(), message))

      await().atMost(com.jayway.awaitility.Duration.ONE_SECOND).until { future1.isDone }
      await().atMost(com.jayway.awaitility.Duration.ONE_SECOND).until { resultsFromQuery1.size >= 2 }


      val future2Done = future2.isDone
      val resultsfromF2 = resultsFromQuery2.toList()

      await().atMost(com.jayway.awaitility.Duration.ONE_SECOND).until { future2.isDone }
      await().atMost(com.jayway.awaitility.Duration.ONE_SECOND).until { resultsFromQuery2.size >= 2 }

      val result = vyne.query("""stream { Movie }""")
         .results.take(2).toList() as List<TypedObject>

      result.should.have.size(2)
   }

   @Test
   fun `multiple queries can consume the same topic concurrently`(): Unit = runBlocking {

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
            @KafkaOperation( topic = "movies", offset = "earliest" )
            operation streamMovieQuery():Stream<Movie>
         }

      """
         )
      ) { schema -> listOf(KafkaInvoker(connectionRegistry, SimpleSchemaProvider(schema))) }

      val message1 = "{\"id\": \"1234\",\"title\": \"Title 1\"}"
      val message2 = "{\"id\": \"5678\",\"title\": \"Title 2\"}"

      kafkaProducer.send(ProducerRecord("movies", UUID.randomUUID().toString(), message1))
      kafkaProducer.send(ProducerRecord("movies", UUID.randomUUID().toString(), message2))

      val result = vyne.query("""stream { Movie }""")
         .results.take(2).toList() as List<TypedObject>

      result.should.have.size(2)
   }

   fun buildQuery(
      vyne: Vyne,
      queryId: String,
      results: MutableList<TypedInstance>
   ): CompletableFuture<List<TypedInstance>> {
      return CompletableFuture.supplyAsync {
         runBlocking {
            val queryContext = vyne.query("""stream { Movie }""")
            queryContext.results
               .onEach {
                  logger.info { "$queryId received event" }
                  results.add(it)
               }
               .take(2)
               .toList()
         }
      }
   }
}

@Configuration
@EnableAutoConfiguration
class KafkaQueryTestConfig

