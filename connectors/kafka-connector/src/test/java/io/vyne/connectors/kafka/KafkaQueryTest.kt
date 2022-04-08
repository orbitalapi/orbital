package io.vyne.connectors.kafka

import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.Vyne
import io.vyne.connectors.kafka.registry.InMemoryKafkaConnectorRegistry
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.protobuf.wire.RepoBuilder
import io.vyne.query.QueryResult
import io.vyne.schemaApi.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.generators.protobuf.TaxiGenerator
import mu.KotlinLogging
import okio.fakefilesystem.FakeFileSystem
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

@SpringBootTest(classes = [KafkaQueryTestConfig::class])
@RunWith(SpringRunner::class)
class KafkaQueryTest : BaseKafkaContainerTest() {

   private val logger = KotlinLogging.logger {}

   lateinit var kafkaProducer: Producer<String, ByteArray>

   lateinit var connectionRegistry: InMemoryKafkaConnectorRegistry


   @Before
   override fun before() {
      super.before()
      val props = Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
      props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducer-${Instant.now().toEpochMilli()}")
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java.name)
      props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000)
      kafkaProducer = KafkaProducer(props)

      connectionRegistry = InMemoryKafkaConnectorRegistry()

      val connection = KafkaConnectionConfiguration(
         "moviesConnection",
         kafkaContainer.bootstrapServers,
         "VyneTest-" + Random.nextInt(),
      )

      connectionRegistry.register(connection)

   }


   @Test
   fun `can use a TaxiQL statement to consume kafka stream`(): Unit = runBlocking {

      val (vyne, _) = vyneWithKafkaInvoker()

      val message1 = "{\"id\": \"1234\",\"title\": \"Title 1\"}"
      val message2 = "{\"id\": \"5678\",\"title\": \"Title 2\"}"

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      val result = vyne.query("""stream { Movie }""")
         .results.take(2).toList() as List<TypedObject>

      result.should.have.size(2)
   }

   @Test
   fun `can consume from the same topic concurrently`(): Unit = runBlocking {

      val (vyne, _) = vyneWithKafkaInvoker()

      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val future1 = buildFiniteQuery(vyne, "query1", resultsFromQuery1)

      val resultsFromQuery2 = mutableListOf<TypedInstance>()
      val future2 = buildFiniteQuery(vyne, "query2", resultsFromQuery1)

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      await().atMost(1, SECONDS).until { future1.isCompleted }
      await().atMost(1, SECONDS).until { resultsFromQuery1.size >= 2 }

      await().atMost(1, SECONDS).until { future2.isCompleted }
      await().atMost(1, SECONDS).until { resultsFromQuery2.size >= 2 }
   }

   @Test
   fun `when query is cancelled, new messages are not propogated`() {
      val (vyne, _) = vyneWithKafkaInvoker()

      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val query = runBlocking { vyne.query("""stream { Movie }""") }
      collectQueryResults(query, resultsFromQuery1)

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      await().atMost(1, SECONDS).until<Boolean> { resultsFromQuery1.size == 2 }
      query.requestCancel()
      Thread.sleep(1000)

      sendMessage(message("message3"))
      sendMessage(message("message4"))

      Thread.sleep(1000)

      resultsFromQuery1.should.have.size(2)
   }

   @Test
   fun `when no active consumers then topic is unsubscribed`() {
      val (vyne, streamManager) = vyneWithKafkaInvoker()
      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val query = runBlocking { vyne.query("""stream { Movie }""") }
      collectQueryResults(query, resultsFromQuery1)

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      await().atMost(1, SECONDS).until<Boolean> { resultsFromQuery1.size == 2 }

      var currentMessageCount = streamManager.getActiveConsumerMessageCounts()
         .values.first().get()

      currentMessageCount.should.equal(2)

      query.requestCancel()

      sendMessage(message("message3"))
      sendMessage(message("message4"))

      // getActiveConsumerMessageCounts() onyl returns topics we're still subscribed to.
      // So should return empty, indicating that an unsubscribe happened
      await().atMost(5, SECONDS).until<Boolean> { streamManager.getActiveConsumerMessageCounts().isEmpty() }

      logger.info { "These should not be received... check the logs..." }
      sendMessage(message("message5"))
      sendMessage(message("message6"))
   }

   @Test
   fun `when query is cancelled, subsequent queries receive new messages`() {
      val (vyne, _) = vyneWithKafkaInvoker()

      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val query1 = runBlocking { vyne.query("""stream { Movie }""") }
      collectQueryResults(query1, resultsFromQuery1)

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      await().atMost(1, SECONDS).until<Boolean> { resultsFromQuery1.size == 2 }
      query1.requestCancel()
      Thread.sleep(1000)

      val resultsFromQuery2 = mutableListOf<TypedInstance>()
      val query2 = runBlocking { vyne.query("""stream { Movie }""") }
      collectQueryResults(query2, resultsFromQuery2)

      sendMessage(message("message3"))
      sendMessage(message("message4"))

      Thread.sleep(1000)

      await().atMost(1, SECONDS).until<Boolean> { resultsFromQuery2.size == 2 }
   }


   @Test
   fun `can consume a protobuf message`() {
      val protoSchema = RepoBuilder()
         .add(
            "hello.proto", """
            syntax = "proto3";

            message HelloWorld {
              string content = 1;
              string senderName = 2;
           }
         """.trimIndent()
         )
         .schema()
      val geeratedTaxi = TaxiGenerator(FakeFileSystem())
         .generate(protobufSchema = protoSchema)
      val serviceTaxi = """
         ${KafkaConnectorTaxi.Annotations.imports}
         ${geeratedTaxi.concatenatedSource}

          @KafkaService( connectionName = "moviesConnection" )
         service HelloService {
            @KafkaOperation( topic = "hello-worlds", offset = "earliest" )
            operation streamGoodThings():Stream<HelloWorld>
         }
      """.trimIndent()
      val taxi = listOf(serviceTaxi)
         .joinToString("\n")
      val (vyne, streamManager) = vyneWithKafkaInvoker(taxi)

      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val query1 = runBlocking { vyne.query("""stream { HelloWorld }""") }
      collectQueryResults(query1, resultsFromQuery1)

      val protoMessage = protoSchema.protoAdapter("HelloWorld", false)
         .encode(
            mapOf(
               "content" to "Hello, world",
               "senderName" to "jimmy"
            )
         )

      sendMessage(protoMessage, topic = "hello-worlds")

      await().atMost(100, SECONDS).until<Boolean> { resultsFromQuery1.size == 1 }

      val message = resultsFromQuery1.first()
         .toRawObject()
      message.should.equal(
         mapOf(
            "content" to "Hello, world",
            "senderName" to "jimmy"
         )
      )

   }

   private fun collectQueryResults(query: QueryResult, resultsFromQuery1: MutableList<TypedInstance>) {
      GlobalScope.async {
         logger.info { "Collecting..." }
         query.results
            .collect {
               resultsFromQuery1.add(it)
               logger.info { "received event - have now captured ${resultsFromQuery1.size} events in result handler" }
            }
      }
   }


   private fun sendMessage(message: ByteArray, topic: String = "movies"): RecordMetadata {
      logger.info { "Sending message to topic $topic" }
      val metadata = kafkaProducer.send(ProducerRecord(topic, UUID.randomUUID().toString(), message))
         .get()
      logger.info { "message sent to topic $topic with offset ${metadata.offset()}" }
      return metadata
   }

   private fun sendMessage(message: String, topic: String = "movies"): RecordMetadata {
      return sendMessage(message.toByteArray(), topic)
   }

   private fun message(messageId: String) = """{ "id": "$messageId"}"""

   private val defaultSchema = """
               ${KafkaConnectorTaxi.Annotations.imports}
               type MovieId inherits String
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

            """.trimIndent()

   private fun vyneWithKafkaInvoker(taxi: String = defaultSchema): Pair<Vyne, KafkaStreamManager> {
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

   fun buildFiniteQuery(
      vyne: Vyne,
      queryId: String,
      results: MutableList<TypedInstance>,
      recordCount: Int = 2
   ): Deferred<List<TypedInstance>> {
      val queryContext = runBlocking {
         vyne.query("""stream { Movie }""")
      }

      return GlobalScope.async {
         val queryContext = vyne.query("""stream { Movie }""")
         queryContext.results
            .onEach {
               logger.info { "$queryId received event" }
               results.add(it)
            }
            .take(recordCount)
            .toList()
      }
   }

}

@Configuration
@EnableAutoConfiguration
class KafkaQueryTestConfig

