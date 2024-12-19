package com.orbitalhq.query.runtime.core.gateway

import com.hazelcast.test.TestHazelcastInstanceFactory
import com.jayway.awaitility.Awaitility
import com.nhaarman.mockito_kotlin.*
import com.orbitalhq.AuthClaimType.AuthClaimsTypeDefinition
import com.orbitalhq.Vyne
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.VyneProvider
import com.orbitalhq.errors.ErrorType.ErrorTypeDefinition
import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.OperationResult
import com.orbitalhq.query.Fact
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
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.TaxiSchema
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
import java.security.Principal
import java.time.Duration
import java.util.concurrent.TimeUnit


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
      setSchema("""
               type CorrelationId inherits String
               
               @com.orbitalhq.formats.Csv
               model CsvModel {
                 givenName : FirstName inherits String
                 surname : LastName inherits String
               }

               model RestResponse {
                 status: ResponseStatus inherits String
               }
               
               service CsvConsumerApi {
                 @taxi.http.HttpOperation(method = "POST", url = "http://localhost:${mockWebServerRule.port}/csv")
                 write operation saveCsv(@taxi.http.RequestBody CsvModel,
                                         @taxi.http.HttpHeader(name = "x-api-correlationId") correlationId: CorrelationId
                                         ): RestResponse
              }
        
             @taxi.http.HttpOperation(method = "POST", url = "$CsvQueryEndPoint")
             query CsvQuery(
               @taxi.http.RequestBody csvModel: CsvModel,
               @taxi.http.HttpHeader(name = "$CorrelationHeaderName") correlationId: CorrelationId) {
                 given { csvModel }
                 call CsvConsumerApi::saveCsv
              }
         """.trimIndent())
      mockWebServerRule.prepareResponse { response ->
         //Response is not important as the operation returns void in the schema.
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody("""{ "status" : "OK" }""")
      }

      Awaitility.await().atMost(60, TimeUnit.SECONDS).until<Boolean> { handler.routes.isNotEmpty() }
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
      setSchema("""
               type CorrelationId inherits String
               type FilmId inherits Int
               
               model Film {
                 filmId: FilmId
               }
      
               model FilmRating {
                 filmId: FilmId
                 rating: Rating inherits String
               }
      
              service FilmRatingsApi {
                     @taxi.http.HttpOperation(method = "GET", url = "http://localhost:${mockWebServerRule.port}/{filmId}")
                     operation filmRating(@taxi.http.PathVariable("fimlId") filmId: FilmId): FilmRating
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
      """.trimIndent())

      mockWebServerRule.prepareResponse { response ->
         //Response is not important as the operation returns void in the schema.
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{ "filmId" : 1, "rating": "Good" }""")
      }

      val orbitalHttpQueryCorrelationId = "film-rating-query-1"

      val result = webClient
         .mutate()
         .responseTimeout(Duration.ofSeconds(10))
         .build()
         .get()
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
      setSchema("""
              type FilmId inherits Int
              type CorrelationId inherits String
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
              
              model StreamProvider {
               filmId: FilmId
               provider: ProviderName inherits String
              }
             
             service StreamProvidersApi {
                @taxi.http.HttpOperation(method = "GET", url = "http://localhost:${mockWebServerRule.port}/streaming/{filmId}")
                operation filmRating(@taxi.http.PathVariable("fimlId") filmId: FilmId): StreamProvider
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
             @taxi.http.HttpOperation(method = "GET", url = "$StreamProvidersQueryEndPoint/{filmId}")
             query FilmRatingQuery(
                @taxi.http.PathVariable("filmId") filmId: FilmId,
                @taxi.http.HttpHeader(name = "$CorrelationHeaderName") correlationId: CorrelationId,
                @taxi.http.ResponseHeader("$CorrelationHeaderName") correlationId: CorrelationId,
                @taxi.http.ResponseHeader("filmId") filmId: FilmId) {
              given { filmId }
              find { StreamProvider }
            }
         """.trimIndent())

      mockWebServerRule.prepareResponse { response ->
         //Response is not important as the operation returns void in the schema.
         response
            .setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{ "filmId" : 1, "provider": "Disney" }""")
      }

      val orbitalHttpQueryCorrelationId = "stream-provider-query-1"

      val result = webClient
         .mutate()
         .responseTimeout(Duration.ofSeconds(10))
         .build()
         .get()
         .uri("$StreamProvidersQueryEndPoint/1")
         .header(CorrelationHeaderName, orbitalHttpQueryCorrelationId)
         .exchange()
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

   @Test
   fun `returns status code correctly when query directly throws`() {
      setSchema("""
             @taxi.http.ResponseCode(400)
             @taxi.http.ResponseBody
             model BadRequestError inherits Error {
               message: ErrorMessage
             }
        
            type ResponseContentType inherits String
        
            @taxi.http.HttpOperation(url = '/api/q/accounts/', method = 'GET')
            query getAccountWithoutId(@taxi.http.ResponseHeader(name = "Content-Type", value = "application/json") contentType: ResponseContentType) { 
               find { throw( (BadRequestError) {
                  message: 'xyz.abc.def'
                })}
            }
         """.trimIndent())

      val result = webClient
         .get()
         .uri("/api/q/accounts/")
         .exchange()
         .expectStatus().isBadRequest
         .expectHeader().value("Content-Type", CoreMatchers.`is`("application/json"))
         .returnResult<Map<String, Any>>()

      val responseBody = result.responseBody.blockLast()
      responseBody["message"].should.equal("xyz.abc.def")
   }

   private fun setSchema(testSchema: String) {
      (schemaProvider as SimpleSchemaProvider).schema = TaxiSchema.fromStrings(
         listOf(AuthClaimsTypeDefinition, ErrorTypeDefinition, testSchema))
      val schema = schemaProvider.schema
      schemaStore.setSchema(schema)
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
         return SimpleSchemaProvider(TaxiSchema.fromStrings(listOf(AuthClaimsTypeDefinition, ErrorTypeDefinition)))
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

         return object: VyneProvider {
            override fun createVyne(facts: Set<Fact>): Vyne {
               return if (facts.isEmpty()) {
                  Vyne(listOf(schemaProvider.schema), queryEngineFactory)
               } else {
                  val clone = Vyne(listOf(schemaProvider.schema), queryEngineFactory)
                  facts.forEach { clone.addModel(it.toTypedInstance(clone.schema), it.factSetId) }
                  clone
               }

            }

            override fun createVyne(facts: Set<Fact>, schema: Schema, queryOptions: QueryOptions): Vyne {
               return if (facts.isEmpty()) {
                  Vyne(listOf(schemaProvider.schema), queryEngineFactory)
               } else {
                  val clone = Vyne(listOf(schemaProvider.schema), queryEngineFactory)
                  facts.forEach { clone.addModel(it.toTypedInstance(clone.schema), it.factSetId) }
                  clone
               }
            }
         }
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

         return QueryService(
            schemaProvider,
            vyneProvider,
            historyEventConsumerProvider,
            Jackson2ObjectMapperBuilder().build(),
            ActiveQueryMonitor(TestHazelcastInstanceFactory().newHazelcastInstance()),
            QueryResponseFormatter(listOf(CsvFormatSpec))
         )
      }

      @Bean
      fun springWebFilterChainNoAuthentication(http: ServerHttpSecurity): SecurityWebFilterChain? {
         return  http
            .csrf { it.disable() }
            .cors { it.disable() }
            .headers { it.disable() }
            .authorizeExchange {
               it.anyExchange().permitAll()
            }.build()
      }
   }
}

