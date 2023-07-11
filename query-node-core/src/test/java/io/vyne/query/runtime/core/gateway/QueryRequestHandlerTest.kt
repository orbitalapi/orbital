package io.vyne.query.runtime.core.gateway

import com.jayway.awaitility.Awaitility
import com.nhaarman.mockito_kotlin.*
import io.kotest.matchers.shouldBe
import io.vyne.schema.consumer.SimpleSchemaStore
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import reactor.core.publisher.Flux
import java.util.concurrent.TimeUnit

//@SpringBootTest
@RunWith(SpringRunner::class)
//@WebFluxTest(QueryRequestHandler::class)
@SpringBootTest(
   classes = [QueryRequestHandlerTest.TestConfig::class],
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = ["spring.main.web-application-type=reactive",
   ]
)
@Ignore("Can't work out how to get this to work")
class QueryRequestHandlerTest {

   @Autowired
   lateinit var webClient: WebTestClient

   @Autowired
   lateinit var schemaStore: SimpleSchemaStore

   @Autowired
   lateinit var handler: QueryRouteService

   @Autowired
   lateinit var queryExecutor: RoutedQueryExecutor

   @Test
   fun handlesRequest() {
      schemaStore.setSchema(
         TaxiSchema.from(
            """

         model Film {
            id : FilmId inherits Int
         }

         service Films {
            operation findFilm(FilmId):Film
         }

         @HttpOperation( method = "GET" , url = "/films/{FilmId}" )
         query FindFilm( @PathVariable("FilmId") filmId : FilmId) {
            find { Film( FilmId == filmId ) }
         }
      """.trimIndent()
         )
      )
      val (vyne, stub) = testVyne(schemaStore.schemaSet.schema.asTaxiSchema())

      Awaitility.await().atMost(2, TimeUnit.SECONDS).until<Boolean> { handler.routes.isNotEmpty() }

      val result = webClient.get().uri("/films/123")
         .accept(MediaType.APPLICATION_JSON)
         .exchange()
         .expectStatus().isOk
         .returnResult<Map<String, Any>>()

      argumentCaptor<RoutedQuery>().apply {
         verify(queryExecutor).handleRoutedQuery(capture())

         lastValue.argumentValues.shouldBe(
            mapOf("filmId" to "123")
         )
      }
   }


   @SpringBootApplication
   @TestConfiguration
   @Import(QueryGatewayRouterConfig::class)
   class TestConfig {

      @Bean
      fun queryExecutor():RoutedQueryExecutor {
         val executor= mock<RoutedQueryExecutor> {
            on { handleRoutedQuery(any())} doReturn Flux.just("Hello")
         }
         return executor
      }

      @Bean
      fun queryRouteService(schemaStore: SimpleSchemaStore, queryExecutor: RoutedQueryExecutor): QueryRouteService {
         return QueryRouteService(schemaStore, queryExecutor)
      }

      @Bean
      fun schemaStore(): SimpleSchemaStore = SimpleSchemaStore()
   }

}

