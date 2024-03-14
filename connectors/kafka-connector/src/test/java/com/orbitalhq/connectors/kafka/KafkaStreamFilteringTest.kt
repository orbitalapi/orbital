package com.orbitalhq.connectors.kafka

import com.jayway.awaitility.Awaitility
import com.orbitalhq.models.TypedInstance
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [KafkaQueryTestConfig::class])
@RunWith(SpringRunner::class)
class KafkaStreamFilteringTest : BaseKafkaContainerTest() {

   private val logger = KotlinLogging.logger {}


   @Before
   override fun before() {
      super.before()
      val (producer, registry) = buildProducer()
      kafkaProducer = producer
      connectionRegistry = registry
   }

   @Test
   fun `can filter a stream from kafka`() {
      val (vyne, _) = vyneWithKafkaInvoker(defaultSchema)
      val resultsFromQuery1 = mutableListOf<TypedInstance>()
      val query = runBlocking { vyne.query("""stream { Movie.filterEach( (MovieTitle) -> MovieTitle == 'Jaws' ) }""") }
      collectQueryResults(query, resultsFromQuery1)

      sendMessage("""{ "id" : "1", "title" : "Star Wars" }""")
      sendMessage("""{ "id" : "2", "title" : "Jaws" }""")
      sendMessage("""{ "id" : "1", "title" : "Star Wars" }""")
      sendMessage("""{ "id" : "2", "title" : "Jaws" }""")

      Awaitility.await().atMost(20, TimeUnit.SECONDS).until<Boolean> { resultsFromQuery1.size >= 2 }
      resultsFromQuery1.shouldHaveSize(2)
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


}
