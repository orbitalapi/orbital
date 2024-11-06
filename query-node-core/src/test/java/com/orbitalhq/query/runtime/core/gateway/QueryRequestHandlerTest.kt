package com.orbitalhq.query.runtime.core.gateway

import com.hazelcast.test.TestHazelcastInstanceFactory
import com.jayway.awaitility.Awaitility
import com.nhaarman.mockito_kotlin.*
import com.orbitalhq.AuthClaimType
import com.orbitalhq.AuthClaimType.AuthClaimsTypeDefinition
import com.orbitalhq.Vyne
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.VyneProvider
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.errors.ErrorType.ErrorTypeDefinition
import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.OperationResult
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.QueryEngineFactory
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.projection.LocalProjectionProvider
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.query.runtime.core.QueryResponseFormatter
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.SimpleVyneProvider
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import com.orbitalhq.spring.invokers.RestTemplateInvoker
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import org.hamcrest.CoreMatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal
import java.time.Duration
import java.util.concurrent.TimeUnit

//@SpringBootTest
@RunWith(SpringRunner::class)
@SpringBootTest(
   classes = [QueryRequestHandlerTest.TestConfig::class],
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "spring.main.web-application-type=reactive",
   ]
)

class QueryRequestHandlerTest {

   @Autowired
   lateinit var webClient: WebTestClient

   @Autowired
   lateinit var schemaStore: SimpleSchemaStore

   @Autowired
   lateinit var handler: QueryRouteService

   @Autowired
   lateinit var queryExecutor: RoutedQueryExecutor

   @Autowired
   lateinit var mockWebServerRule: MockWebServerRule


   @Autowired
   lateinit var schemaProvider: SchemaProvider

   companion object {
      const val CsvQueryEndPoint = "/api/q/csv"
      const val FilmRatingQueryEndPoint = "/api/q/films"
      const val CorrelationHeaderName = "x-api-correlationId"
      const val StreamProvidersQueryEndPoint = "/api/q/streamProviders"
   }

   @Test
   fun `can send csv in payload and project and get a json response back as part of vyne http endpoint query`() {
      val schema = schemaProvider.schema
      schemaStore.setSchema(schema)
      mockWebServerRule.prepareResponse { response ->
         //Response is not important as the operation returns void in the schema.
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody("""{ "status" : "OK" }""")
      }

      Awaitility.await().atMost(60000, TimeUnit.SECONDS).until<Boolean> { handler.routes.isNotEmpty() }
      val orbitalHttpQueryCorrelationId = "correlationId-1"
      val result = webClient.post()
         .uri(CsvQueryEndPoint)
         .contentType(MediaType.parseMediaType("text/csv"))
         .header(CorrelationHeaderName, orbitalHttpQueryCorrelationId)
         .body(BodyInserters.fromValue("givenName,surname\nfoo,bar"))
         .exchange()
         .expectStatus().isOk
         .returnResult<Map<String, Any>>()

      val responseBody = result.responseBody.blockLast()
      responseBody["status"].should.equal("OK")
      mockWebServerRule.takeRequest().headers[CorrelationHeaderName]!!.should.equal(orbitalHttpQueryCorrelationId)
   }

   @Test
   fun `can accept value through request headers and echo them back in response headers`() {
      val schema = schemaProvider.schema
      schemaStore.setSchema(schema)
      mockWebServerRule.prepareResponse { response ->
         //Response is not important as the operation returns void in the schema.
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{ "filmId" : 1, "rating": "Good" }""")
      }

      val orbitalHttpQueryCorrelationId = "film-rating-query-1"

      val result = webClient.get()
         .uri("$FilmRatingQueryEndPoint/1")
         .header(CorrelationHeaderName, orbitalHttpQueryCorrelationId)
         .exchange()
         .expectStatus().isOk
         .expectHeader().value(CorrelationHeaderName, CoreMatchers.`is`(orbitalHttpQueryCorrelationId))
         .expectHeader().value("Content-Type", CoreMatchers.`is`("application/json"))
         .expectHeader().value("x-vyne-query-id", CoreMatchers.startsWith("routed"))
         .expectHeader().value("x-vyne-client-query-id", CoreMatchers.startsWith("routed"))
         .expectHeader().value("filmId", CoreMatchers.`is`("1"))
         .returnResult<Map<String, Any>>()

      val responseBody = result.responseBody.blockLast()
      responseBody["rating"].should.equal("Good")
      mockWebServerRule.takeRequest()
   }

   @Test
   fun `can still return response headers in case there is a policy error`() {
      val schema = schemaProvider.schema
      schemaStore.setSchema(schema)
      mockWebServerRule.prepareResponse { response ->
         //Response is not important as the operation returns void in the schema.
         response
            .setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{ "filmId" : 1, "provider": "Disney" }""")
      }

      val orbitalHttpQueryCorrelationId = "stream-provider-query-1"

      val result = webClient
         .mutate()
         .responseTimeout(Duration.ofMinutes(30000))
         .build()
         .get()
         .uri("$StreamProvidersQueryEndPoint/1")
         .header(CorrelationHeaderName, orbitalHttpQueryCorrelationId)
         .exchange()
//            .expectStatus().isBadRequest
         .expectHeader().value(CorrelationHeaderName, CoreMatchers.`is`(orbitalHttpQueryCorrelationId))
         .expectHeader().value("Content-Type", CoreMatchers.`is`("application/json"))
         .expectHeader().value("filmId", CoreMatchers.`is`("1"))
         .returnResult<Map<String, Any>>()

      result.responseBody.blockLast().shouldBe(
         mapOf(
            "Code" to "xyz.abc.def",
            "Id" to "correlationId",
            "Message" to "Invalid API Status",
            "Errors" to listOf("An unexpected error occurred")
         )
      )
      mockWebServerRule.takeRequest()

   }


   @SpringBootApplication
   @TestConfiguration
   @Import(QueryGatewayRouterConfig::class)
   class TestConfig {
      @Rule
      @JvmField
      final val server = MockWebServerRule()

      @Bean
      fun queryRouteService(
         schemaStore: SimpleSchemaStore,
         queryExecutor: RoutedQueryExecutor,
         metricsReporter: QueryMetricsReporter
      ): QueryRouteService {
         return QueryRouteService(schemaStore, queryExecutor, metricsReporter = metricsReporter)
      }

      @Bean
      fun mockWebServerRule(): MockWebServerRule {
         return this.server
      }

      @Bean
      fun schemaStore(): SimpleSchemaStore = SimpleSchemaStore()

      @Bean
      fun queryMetricsReporter() = com.orbitalhq.metrics.NoOpMetricsReporter

      @Bean
      @Primary
      fun schemaProvider(): SchemaProvider {

         return SimpleSchemaProvider(
            TaxiSchema.fromStrings(
               listOf(
                  AuthClaimsTypeDefinition, ErrorTypeDefinition,
                  """
         type CorrelationId inherits String
         type FilmId inherits Int
         @com.orbitalhq.formats.Csv
         model CsvModel {
           givenName : FirstName inherits String
           surname : LastName inherits String
         }

         model RestResponse {
           status: ResponseStatus inherits String
         }

         model Film {
           filmId: FilmId
         }

         model FilmRating {
           filmId: FilmId
           rating: Rating inherits String
         }

         model StreamProvider {
           filmId: FilmId
           provider: ProviderName inherits String
         }

         enum ErrorEnum {
           TR_ORBITAL_UnexpectedError("An unexpected error occurred")
         }

         model OrbitalBaseError inherits com.orbitalhq.errors.Error {
           Code: OrbitalErrorCode inherits String
           Id: OrbitalErrorId inherits String
           Message: OrbitalErrorMessage inherits String
           Errors: ErrorEnum[]
         }

         @taxi.http.ResponseBody
         model OrbitalUnexpectedError inherits OrbitalBaseError {
           Code: OrbitalErrorCode inherits String
           Id: OrbitalErrorId inherits String
           Message: OrbitalErrorMessage inherits String
           Errors: ErrorEnum[]
         }

         policy AllAccessStreamProviders against StreamProvider (filmId : FilmId, correlationId: CorrelationId?) -> {
            read {
               when {
                  filmId == 2 -> StreamProvider
                  else ->  throw( (OrbitalUnexpectedError) {
                  Code: 'xyz.abc.def',
                  Message: 'Invalid API Status',
                  Id: "correlationId",
                  Errors: [ErrorEnum.TR_ORBITAL_UnexpectedError]
                })
               }
            }
         }

        service CsvConsumerApi {
           @taxi.http.HttpOperation(method = "POST", url = "http://localhost:${server.port}/csv")
           write operation saveCsv(@taxi.http.RequestBody CsvModel,
                                   @taxi.http.HttpHeader(name = "x-api-correlationId") correlationId: CorrelationId
                                   ): RestResponse
        }

        service FilmRatingsApi {
               @taxi.http.HttpOperation(method = "GET", url = "http://localhost:${server.port}/{filmId}")
               operation filmRating(@taxi.http.PathVariable("fimlId") filmId: FilmId): FilmRating
        }

        service StreamProvidersApi {
           @taxi.http.HttpOperation(method = "GET", url = "http://localhost:${server.port}/streaming/{filmId}")
               operation filmRating(@taxi.http.PathVariable("fimlId") filmId: FilmId): StreamProvider
        }

        @taxi.http.HttpOperation(method = "POST", url = "$CsvQueryEndPoint")
        query CsvQuery(
         @taxi.http.RequestBody csvModel: CsvModel,
         @taxi.http.HttpHeader(name = "$CorrelationHeaderName") correlationId: CorrelationId) {
           given { csvModel }
           call CsvConsumerApi::saveCsv
        }

        @taxi.http.HttpOperation(method = "GET", url = "$FilmRatingQueryEndPoint/{filmId}")
        query FilmRatingQuery(
             @taxi.http.PathVariable("filmId") filmId: FilmId,
             @taxi.http.HttpHeader(name = "$CorrelationHeaderName") correlationId: CorrelationId,
             @taxi.http.ResponseHeader("$CorrelationHeaderName") correlationId: CorrelationId,
             @taxi.http.ResponseHeader("filmId") filmId: FilmId) {
              given { filmId}
              find { FilmRating }
            }
        @taxi.http.HttpOperation(method = "GET", url = "$StreamProvidersQueryEndPoint/{filmId}")
        query FilmRatingQuery(
             @taxi.http.PathVariable("filmId") filmId: FilmId,
             @taxi.http.HttpHeader(name = "$CorrelationHeaderName") correlationId: CorrelationId,
             @taxi.http.ResponseHeader("$CorrelationHeaderName") correlationId: CorrelationId,
             @taxi.http.ResponseHeader("filmId") filmId: FilmId) {
              given { filmId }
              find { StreamProvider }
            }
      """.trimIndent()
               )
            )
         )
      }


      @Bean
      @Primary
      fun vyneProvider(schemaProvider: SchemaProvider): VyneProvider {
         val restTemplateInvoker = RestTemplateInvoker(
            schemaProvider,
            WebClient.builder(),
            AuthWebClientCustomizer.empty()
         )
         val queryEngineFactory =
            QueryEngineFactory.withOperationInvokers(
               VyneCacheConfiguration.default(),
               formatSpecs = emptyList(),
               invokers = listOf(restTemplateInvoker),
               projectionProvider = LocalProjectionProvider(),
               stateStoreProvider = null
            )
         val vyne = Vyne(listOf(schemaProvider.schema), queryEngineFactory)
         return SimpleVyneProvider(vyne)
      }

      @Bean
      fun streamResultStreamProvider(): StreamResultStreamProvider {
         val streamResultStreamProvider = object : StreamResultStreamProvider {
            override fun getResultStream(streamName: String, principal: Principal?): Flux<Any> {
               TODO("Not yet implemented")
            }

         }
         return streamResultStreamProvider
      }

      @Bean
      fun queryService(
         vyneProvider: VyneProvider,
         schemaProvider: SchemaProvider
      ): QueryService {

         val queryEventConsumer = object : QueryEventConsumer {
            override fun handleEvent(event: QueryEvent) {

            }

            override fun recordResult(operation: OperationResult, queryId: String) {

            }

         }

         val historyEventConsumerProvider = object : HistoryEventConsumerProvider {
            override fun createEventConsumer(queryId: String, schema: Schema): QueryEventConsumer {
               return queryEventConsumer
            }

         }

         val queryService = QueryService(
            SimpleSchemaProvider(schemaProvider.schema),
            vyneProvider,
            historyEventConsumerProvider,
            Jackson2ObjectMapperBuilder().build(),
            ActiveQueryMonitor(TestHazelcastInstanceFactory().newHazelcastInstance()),
            QueryResponseFormatter(listOf(CsvFormatSpec))
         )
         return queryService
      }

      @Bean
      fun springWebFilterChainNoAuthentication(http: ServerHttpSecurity): SecurityWebFilterChain? {

         return http
            .csrf().disable()
            .cors().disable()
            .headers().disable()
            .authorizeExchange()
            .anyExchange().permitAll()
            .and()
            .build()
      }
   }

}

