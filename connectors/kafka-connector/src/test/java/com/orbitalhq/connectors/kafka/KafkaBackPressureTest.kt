package com.orbitalhq.connectors.kafka

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.winterbe.expekt.should
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(classes = [KafkaQueryTestConfig::class])
@RunWith(SpringRunner::class)
class KafkaBackPressureTest : BaseKafkaContainerTest() {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   @Before
   override fun before() {
      super.before()
      val (producer, registry) = buildProducer()
      kafkaProducer = producer
      connectionRegistry = registry
   }
   val schema = """
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

   @Test
   fun `when slow consumer and heavy load then kafka messages are not dropped`(): Unit = runBlocking {

      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(schema)
      // Create a query first to establish the subscription
      val result: Flow<TypedInstance> = vyne.query("""stream { Movie }""").results

      val messageReceivedCount = AtomicInteger(0)
      // Start a very slow collector
      val collectionJob = launch {
         result.collect { message ->
            messageReceivedCount.incrementAndGet()
            delay(2000) // 2 seconds per message - extremely slow
         }
      }

      // Give it a moment to start
      delay(100)

      // Now flood with messages faster than they can be consumed
      launch {
         repeat(1000) { i ->
            sendMessage("""{ "id": "movie-$i", "title" : "Movie $i" }""".toByteArray())
            delay(10) // Send one every 10ms - much faster than 2000ms consumption
         }
      }

      // Let it run for a bit
      delay(10000) // 10 seconds
      val droppedMessages = kafkaStreamManager.getDroppedMessageCounts()
      collectionJob.cancel()
      messageReceivedCount.get().shouldBeGreaterThan(0)
      droppedMessages.shouldBeEmpty()
   }

}
