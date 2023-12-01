package com.orbitalhq.connectors.kafka

import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import com.orbitalhq.Vyne
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.NamedTemplateConnection
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.InMemoryKafkaConnectorRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.protobuf.wire.RepoBuilder
import com.orbitalhq.query.QueryResult
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import com.zaxxer.hikari.HikariConfig
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.generators.protobuf.TaxiGenerator
import mu.KotlinLogging
import okio.fakefilesystem.FakeFileSystem
import org.apache.kafka.clients.producer.*
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

@SpringBootTest(classes = [KafkaQueryTestConfig::class])
@RunWith(SpringRunner::class)
class KafkaQueryTest : BaseKafkaContainerTest() {

   private val logger = KotlinLogging.logger {}


   @Before
   override fun before() {
      super.before()
      val (producer, registry) = buildProducer()
      kafkaProducer = producer
      connectionRegistry = registry
   }


   @Test
   fun `can use a TaxiQL statement to consume kafka stream`(): Unit = runBlocking {

      val (vyne, _) = vyneWithKafkaInvoker(defaultSchema)

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

      val (vyne, _) = vyneWithKafkaInvoker(defaultSchema)

      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val future1 = buildFiniteQuery(vyne, "query1", resultsFromQuery1)

      val resultsFromQuery2 = mutableListOf<TypedInstance>()
      val future2 = buildFiniteQuery(vyne, "query2", resultsFromQuery1)

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      await().atMost(10, SECONDS).until { future1.isCompleted }
      await().atMost(10, SECONDS).until { resultsFromQuery1.size >= 2 }

      await().atMost(10, SECONDS).until { future2.isCompleted }
      await().atMost(10, SECONDS).until { resultsFromQuery2.size >= 2 }
   }

   @Test
   fun `when query is cancelled, new messages are not propogated`() {
      val (vyne, _) = vyneWithKafkaInvoker(defaultSchema)

      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val query = runBlocking { vyne.query("""stream { Movie }""") }
      collectQueryResults(query, resultsFromQuery1)

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      await().atMost(10, SECONDS).until<Boolean> { resultsFromQuery1.size == 2 }
      query.requestCancel()
      Thread.sleep(1000)

      sendMessage(message("message3"))
      sendMessage(message("message4"))

      Thread.sleep(1000)

      resultsFromQuery1.should.have.size(2)
   }

   @Test
   fun `when no active consumers then topic is unsubscribed`() {
      val (vyne, streamManager) = vyneWithKafkaInvoker(defaultSchema)
      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val query = runBlocking { vyne.query("""stream { Movie }""") }
      collectQueryResults(query, resultsFromQuery1)

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      await().atMost(10, SECONDS).until<Boolean> { resultsFromQuery1.size == 2 }

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
      val (vyne, _) = vyneWithKafkaInvoker(defaultSchema)

      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val query1 = runBlocking { vyne.query("""stream { Movie }""") }
      collectQueryResults(query1, resultsFromQuery1)

      sendMessage(message("message1"))
      sendMessage(message("message2"))

      await().atMost(15, SECONDS).until<Boolean> { resultsFromQuery1.size == 2 }
      query1.requestCancel()
      Thread.sleep(1000)

      val resultsFromQuery2 = mutableListOf<TypedInstance>()
      val query2 = runBlocking { vyne.query("""stream { Movie }""") }
      collectQueryResults(query2, resultsFromQuery2)

      sendMessage(message("message3"))
      sendMessage(message("message4"))

      Thread.sleep(1000)

      await().atMost(15, SECONDS).until<Boolean> { resultsFromQuery2.size == 2 }
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

      await().atMost(10, SECONDS).until<Boolean> { resultsFromQuery1.size == 1 }

      val message = resultsFromQuery1.first()
         .toRawObject()
      message.should.equal(
         mapOf(
            "content" to "Hello, world",
            "senderName" to "jimmy"
         )
      )
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
                  stream streamMovieQuery:Stream<Movie>
               }

            """.trimIndent()



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

