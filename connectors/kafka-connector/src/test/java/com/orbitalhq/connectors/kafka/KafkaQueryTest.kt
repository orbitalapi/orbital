package com.orbitalhq.connectors.kafka

import com.jayway.awaitility.Awaitility.await
import com.orbitalhq.Vyne
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.protobuf.wire.RepoBuilder
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.assertions.timing.eventually
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.generators.protobuf.TaxiGenerator
import mu.KotlinLogging
import okio.fakefilesystem.FakeFileSystem
import org.apache.kafka.common.header.internals.RecordHeader
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit4.SpringRunner
import reactor.test.StepVerifier
import java.math.BigInteger
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds


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

      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(defaultSchema)
      sendMessage(message("message1"))
      sendMessage(message("message2"))


      val result = vyne.query(
         """
         stream { Movie }"""
            .trimIndent()
      )

         .results.take(2).toList() as List<TypedObject>
      result.should.have.size(2)
   }

   @Test
   fun `can read Kafka message key into message payload`(): Unit = runBlocking {
      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(
         """
         ${KafkaConnectorTaxi.Annotations.imports}

         import com.orbitalhq.kafka.KafkaMessageKey
         import com.orbitalhq.kafka.KafkaHeader

         model Movie {
            @KafkaMessageKey
            id : MovieId inherits String
            title : Title inherits String
         }
         @KafkaService( connectionName = "moviesConnection" )
         service MovieService {
            @KafkaOperation( topic = "movies", offset = "earliest" )
            stream streamMovieQuery:Stream<Movie>
         }
      """.trimIndent()
      )

      sendMessage("""{ "title" : "Star Wars" }""".toByteArray(), key = "sw-IV")

      val result = vyne.query(
         """
         stream { Movie }"""
            .trimIndent()
      )

         .results.take(1).toList() as List<TypedObject>

      result.should.have.size(1)
      result.single().toRawObject()
         .shouldBe(
            mapOf("id" to "sw-IV", "title" to "Star Wars")
         )
   }

    @Test
    fun `When Kafka subscription fails streaming query returns error`(): Unit = runBlocking {
        val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(
            """
         ${KafkaConnectorTaxi.Annotations.imports}

         import com.orbitalhq.kafka.KafkaMessageKey
         import com.orbitalhq.kafka.KafkaHeader

         model Movie {
            @KafkaMessageKey
            id : MovieId inherits String
            title : Title inherits String
         }
         @KafkaService( connectionName = "invalidConnection" )
         service MovieService {
            @KafkaOperation( topic = "movies", offset = "earliest" )
            stream streamMovieQuery:Stream<Movie>
         }
      """.trimIndent()
        )

        sendMessage("""{ "title" : "Star Wars" }""".toByteArray(), key = "sw-IV")

        try {
            val result = vyne.query(
                """
         stream { Movie }"""
                    .trimIndent()
            ) .results.take(1).toList() as List<TypedObject>
        } catch (e: Exception ) {
            e.message.should.equal("Failed to construct admin client for connection invalidConnection: Failed to create new KafkaAdminClient - JAAS config entry not terminated by semi-colon")
        }
    }

   @Test
   fun `can read Kafka message metadata key into message payload`(): Unit = runBlocking {
      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(
         """
         ${KafkaConnectorTaxi.Annotations.imports}

         import com.orbitalhq.kafka.KafkaMessageKey
         import com.orbitalhq.kafka.KafkaHeader
         import com.orbitalhq.kafka.KafkaMessageMetadata
         import com.orbitalhq.kafka.KafkaMetadataType

         model Movie {
            @KafkaMessageMetadata(KafkaMetadataType.Offset)
            offset : Int
            @KafkaMessageMetadata(KafkaMetadataType.Timestamp)
            timestamp : Long
            @KafkaMessageMetadata(KafkaMetadataType.TimestampType)
            timestampType : String
            @KafkaMessageMetadata(KafkaMetadataType.Partition)
            partition : Int


            title : Title inherits String
         }
         @KafkaService( connectionName = "moviesConnection" )
         service MovieService {
            @KafkaOperation( topic = "movies", offset = "earliest" )
            stream streamMovieQuery:Stream<Movie>
         }
      """.trimIndent()
      )

      sendMessage("""{ "title" : "Star Wars" }""".toByteArray(), key = "sw-IV")

      val result = vyne.query(
         """
         stream { Movie }"""
            .trimIndent()
      )

         .results.take(1).toList() as List<TypedObject>

      result.should.have.size(1)
      val typedObject = result.single()
         .toRawObject() as Map<String, Any>
      typedObject["offset"].shouldBe(0)
      typedObject["timestamp"].shouldBeInstanceOf<BigInteger>()
         .toLong()
         .shouldBeGreaterThan(1718000000000) // a date in the past, but indicates we got a valid timestamp
      typedObject["timestampType"].shouldBe("CreateTime")
      typedObject["partition"].shouldBe(0)
      typedObject["title"].shouldBe("Star Wars")
   }

   @Test
   fun `can read Kafka message headers into message payload`(): Unit = runBlocking {
      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(
         """
         ${KafkaConnectorTaxi.Annotations.imports}

         import com.orbitalhq.kafka.KafkaMessageKey
         import com.orbitalhq.kafka.KafkaHeader
         import com.orbitalhq.kafka.KafkaMessageMetadata
         import com.orbitalhq.kafka.KafkaMetadataType

         model Movie {
            @KafkaHeader("correlationId")
            correlationId : CorrelationId inherits String
            title : Title inherits String
         }
         @KafkaService( connectionName = "moviesConnection" )
         service MovieService {
            @KafkaOperation( topic = "movies", offset = "earliest" )
            stream streamMovieQuery:Stream<Movie>
         }
      """.trimIndent()
      )

      sendMessage("""{ "title" : "Star Wars" }""".toByteArray(), headers = listOf(
         RecordHeader("correlationId", "24601".toByteArray())
      ))

      val result = vyne.query(
         """
         stream { Movie }"""
            .trimIndent()
      )

         .results.take(1).toList() as List<TypedObject>

      result.should.have.size(1)
      result.single().toRawObject()
         .shouldBe(mapOf(
            "correlationId" to "24601",
            "title" to "Star Wars",
         ))
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

      await().atMost(20, SECONDS).until<Boolean> { resultsFromQuery1.size == 2 }

      val currentMessageCount = streamManager.getActiveConsumerMessageCounts()
         .values.first().get()

      currentMessageCount.should.equal(2)

      query.requestCancel()

      sendMessage(message("message3"))
      sendMessage(message("message4"))

      // getActiveConsumerMessageCounts() onyl returns topics we're still subscribed to.
      // So should return empty, indicating that an unsubscribe happened
      await().atMost(20, SECONDS).until<Boolean> { streamManager.getActiveConsumerMessageCounts().isEmpty() }

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

          @KafkaService( connectionName = "moviesConnection" )
         service HelloService {
            @KafkaOperation( topic = "hello-worlds", offset = "earliest" )
            operation streamGoodThings():Stream<HelloWorld>
         }
      """.trimIndent()
      val schema = TaxiSchema.fromStrings(geeratedTaxi.taxi + serviceTaxi + KafkaConnectorTaxi.schema)
      val (vyne, _) = vyneWithKafkaInvoker(schema)

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

   @Test
   fun `when there are two active queries with same stream consumer id only one of them gets the data`(): Unit =
      runBlocking {

         val singlePartitionTopic = "films_${Random.nextInt()}"
         createTopic(singlePartitionTopic)
         val moviesExSchema = """
               ${KafkaConnectorTaxi.Annotations.imports}
               type MovieId inherits String
               type MovieTitle inherits String

               model Movie {
                  id : MovieId
                  title : MovieTitle
               }

               @KafkaService( connectionName = "moviesConnection" )
               service MovieService {
                  @KafkaOperation( topic = "$singlePartitionTopic", offset = "earliest" )
                  stream streamMovieQuery:Stream<Movie>
               }

            """.trimIndent()
         val (vyne1, _) = vyneWithKafkaInvoker(moviesExSchema)
         (0..9).forEach {
            sendMessage(message("message$it"), singlePartitionTopic)
         }

         val result = vyne1.query(
            """
         @StreamConsumer(id = "123")
         stream { Movie }"""
               .trimIndent()
         )
            .results
            .map { "query1" to it }

         val (vyne2, _) = vyneWithKafkaInvoker(moviesExSchema)
         val result2 = vyne2.query(
            """
         @StreamConsumer(id = "123")
         stream { Movie }"""
               .trimIndent()
         )
            .results
            .map { "query2" to it }

         val mergedResults = merge(result, result2).take(10).toList()

         val groupByQuery = mergedResults.groupBy { it.first }
         groupByQuery.keys.size.should.equal(1)
      }


   @Test
   fun `A test where there are two active queries with different StreamConsumer id values, to show that the records are received by both`(): Unit =
      runBlocking {
         val singlePartitionTopic = "arthouse_${Random.nextInt()}"
         createTopic(singlePartitionTopic)
         val moviesExSchema = """
               ${KafkaConnectorTaxi.Annotations.imports}
               type MovieId inherits String
               type MovieTitle inherits String
               type ReleaseDate inherits Instant

               model Movie {
                  id : MovieId
                  title : MovieTitle
                  releaseDate: ReleaseDate?
               }

               @KafkaService( connectionName = "moviesConnection" )
               service MovieService {
                  @KafkaOperation( topic = "$singlePartitionTopic", offset = "earliest" )
                  stream streamMovieQuery:Stream<Movie>
               }

            """.trimIndent()
         val (vyne1, _) = vyneWithKafkaInvoker(moviesExSchema)
         (0..9).forEach {
            sendMessage(message("message$it"), singlePartitionTopic)
         }

         val result = vyne1.query(
            """
         @StreamConsumer(id = "123")
         stream { Movie }"""
               .trimIndent()
         )
            .results
            .map { "query1" to it }

         val (vyne2, _) = vyneWithKafkaInvoker(moviesExSchema)
         val result2 = vyne2.query(
            """
         @StreamConsumer(id = "1234")
         stream { Movie }"""
               .trimIndent()
         )
            .results
            .map { "query2" to it }

         val recievedResults = mutableListOf<Pair<String,TypedInstance>>()
         val mergedResults = merge(result, result2).take(20)
            .toList(recievedResults)
         eventually(10.seconds) {
            val receivedPerQuery = recievedResults.groupBy { it.first }
            // 10 results should be received on both query1 and query2
            receivedPerQuery.keys.size == 2 && receivedPerQuery.all { it.value.size == 10 }
         }.shouldBeTrue()
      }

   @Test
   fun `subscription is not cancelled when there is a parsing exception`(): Unit = runBlocking {

      val topic = "arrivals"
      val (vyne, kafkaStreamManager, stub, streamErrors) = vyneWithKafkaInvoker(
         """
               ${KafkaConnectorTaxi.Annotations.imports}
                [[ Flight Number]]
                type FlightNum inherits String
               [[ Airport ]]
               type Airport inherits String
               [[ Terminal ]]
               type Terminal inherits String
               [[ Gate ]]
               type Gate inherits String
               [[ Arrival/Departure Time]]
               type FlightDate inherits Date
               [[ Arrival/Departure Time]]
               @Format("yyyy-MM-dd HH:mm:ss")
               type FlightTime inherits DateTime
               [[ TraveTime in minutes]]
               type TravelTime inherits Int
               [[ Event Time]]
               @Format("yyyy-MM-dd'T'HH:mm:ssXXX")
               type EventTime inherits Instant

               model Arrival {
                event_time: EventTime
                day: FlightDate
                flight: FlightNum
                airport: Airport
                arrival_gate: Gate
                arrival_time: FlightTime
               }

               @KafkaService( connectionName = "moviesConnection" )
               service MovieService {
                  @KafkaOperation( topic = "$topic", offset = "earliest" )
                  stream streamMovieQuery:Stream<Arrival>
               }

            """.trimIndent()
      )

      val p = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
         .parse("2024-03-04T16:57:06+00:00")

      fun arrivalMessage(eventTime: String) = """
         {
           "event_time": "$eventTime",
           "day": "2023-07-04",
           "flight": "LH361",
           "airport": "ATL",
           "arrival_gate": "C90",
           "arrival_time": "2023-07-04 11:23:00"
         }
      """.trimIndent()
      // This will cause TypedInstance parse exception.
      sendMessage(arrivalMessage("2024-03-04T16:57:06.555+00:00"), topic)
      // This is a valid message
      sendMessage(arrivalMessage("2024-03-04T16:57:06+00:00"), topic)

      val result = vyne.query(
         """
         stream { Arrival }"""
            .trimIndent()
      )

         .results.take(1).toList() as List<TypedObject>

      StepVerifier.create(streamErrors.errors.take(1))
         .expectNextMatches { streamError ->
            streamError.error.message == "Failed to parse value 2024-03-04T16:57:06.555+00:00 to type EventTime with formats yyyy-MM-dd'T'HH:mm:ssXXX - Text '2024-03-04T16:57:06.555+00:00' could not be parsed, unparsed text found at index 23"
         }
         .expectComplete()
         .verify()
      result.should.have.size(1)


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

