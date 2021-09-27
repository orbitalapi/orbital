package io.vyne.connectors.kafka

import com.winterbe.expekt.should
import io.vyne.query.VyneQlGrammar
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(classes = [KafkaQueryTestConfig::class])
@RunWith(SpringRunner::class)
class KafkaQueryTest {

   @Before
   fun setup() {
   }

   @Test
   fun canStart() {
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

         @KafkaService( topic = "movies", offset = "earliest" )
         service MovieService {
            operation streamMovieQuery():Stream<Movie>
         }

      """
         )
      ) { schema -> listOf(KafkaInvoker(SimpleSchemaProvider(schema))) }

      val result = vyne.query("""stream { Movie }""")
         .typedObjects()

      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "id" to 1))
   }
}

@Configuration
@EnableAutoConfiguration
class KafkaQueryTestConfig

