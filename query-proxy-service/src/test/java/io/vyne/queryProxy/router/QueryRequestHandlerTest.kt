package io.vyne.queryProxy.router

import com.jayway.awaitility.Awaitility
import io.vyne.queryProxy.WebRouteConfig
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@WebFluxTest(QueryRequestHandler::class)
@Import(WebRouteConfig::class)
class QueryRequestHandlerTest {

   @Autowired
   lateinit var webClient: WebTestClient

   @Autowired
   lateinit var schemaStore: SimpleSchemaStore

   @Autowired
   lateinit var handler: QueryRequestHandler

   @Test
   fun handlesRequest() {
      schemaStore.setSchema(
         TaxiSchema.from(
            """

         model Film {
            id : FilmId inherits Int
         }

         @HttpOperation( method = "GET" , url = "/films/{FilmId}" )
         query FindFilm( @PathVariable("FilmId") filmId : FilmId) {
            find { Film( FilmId == filmId ) }
         }
      """.trimIndent()
         )
      )
      val (vyne,stub) = testVyne(schemaStore.schemaSet.schema.asTaxiSchema())

      Awaitility.await().atMost(2, TimeUnit.SECONDS).until<Boolean> { handler.routes.isNotEmpty() }

      val result = webClient.get().uri("/films/123")
         .accept(MediaType.APPLICATION_JSON)
         .exchange()
         .expectStatus().isOk
         .returnResult<Map<String, Any>>()
      TODO()
   }


   @TestConfiguration
   class TestConfig {
      @Bean
      fun schemaStore(): SimpleSchemaStore = SimpleSchemaStore()
   }

}

