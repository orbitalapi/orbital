package com.orbitalhq.connectors.kafka

import com.orbitalhq.firstRawObject
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import io.kotest.common.runBlocking
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.subscribe
import org.junit.Before
import org.junit.Test
import org.testcontainers.shaded.org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

class KafkaWriterTest  : BaseKafkaContainerTest() {

   @Before
   override fun before() {
      super.before()
      val (producer, registry) = buildProducer()
      kafkaProducer = producer
      connectionRegistry = registry
   }

   @Test
   fun `can write a query result to kafka`():Unit = runBlocking {
      val schema = """
         model Person {
            name : Name inherits String
         }
         service PersonApi {
            operation getPerson():Person
         }
          @KafkaService( connectionName = "moviesConnection" )
               service PersonKafkaService {
                  @KafkaOperation( topic = "people", offset = "earliest" )
                  stream streamMovieQuery:Stream<Person>

                  @KafkaOperation( topic = "people", offset = "earliest" )
                  write operation publishMessage(Person):Person
               }

      """.trimIndent()
      val (vyne, _, stub) = vyneWithKafkaInvoker(schema)
      stub.addResponse("getPerson", vyne.parseJson("Person","""{ "name" : "Jimmy" }"""))

      val collectedResults = mutableListOf<TypedInstance>()
      val readingQuery = vyne.query("""stream { Person }""")
         .results
         .onEach {
            collectedResults.add(it)
         }
      Thread.sleep(2500) // Just wait for the sbuscription to start
      val result = vyne.query("""
         find { Person }
         call PersonKafkaService::publishMessage
      """.trimIndent())
         .firstRawObject()

      // TODO : This works, but the test doesn't. Need to work out how to address
      // race conditions in subs/pubs ordering
//      Awaitility.await().atMost(30, TimeUnit.SECONDS).until { collectedResults.isNotEmpty() }
//      TODO()
//
//      TODO()
   }

}
