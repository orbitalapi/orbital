package com.orbitalhq.query.runtime.core.gateway

import com.jayway.awaitility.Awaitility
import com.nhaarman.mockito_kotlin.*
import com.orbitalhq.AuthClaimType.AuthClaimsTypeDefinition
import com.orbitalhq.Vyne
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.VyneProvider
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
    lateinit var schemaProvider: SchemaProvider

   @Test
   fun `can send csv in payload and project and get a json response back as part of vyne http endpoint query`() {
       val schema = schemaProvider.schema
      schemaStore.setSchema(schema)

      Awaitility.await().atMost(60000, TimeUnit.SECONDS).until<Boolean> { handler.routes.isNotEmpty() }

      val result = webClient.post()
         .uri("/api/q/csv")
         .contentType(MediaType.parseMediaType("text/csv"))
         .body(BodyInserters.fromValue("givenName,surname\nfoo,bar"))
         .exchange()
         .expectStatus().isOk
         .returnResult<Map<String, Any>>()

      val responseBody = result.responseBody.blockLast()
      responseBody["status"].should.equal("OK")
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
       fun schemaStore(): SimpleSchemaStore = SimpleSchemaStore()

       @Bean
       fun queryMetricsReporter() = com.orbitalhq.metrics.NoOpMetricsReporter

       @Bean
       @Primary
       fun schemaProvider(): SchemaProvider {
           server.prepareResponse { response ->
               //Response is not important as the operation returns void in the schema.
               response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody("""{ "status" : "OK" }""")
           }
           return SimpleSchemaProvider(
               TaxiSchema.fromStrings(
                   listOf(AuthClaimsTypeDefinition,
                       """
         @com.orbitalhq.formats.Csv
         model CsvModel {
           givenName : FirstName inherits String
           surname : LastName inherits String
         }
         
         model RestResponse {
           status: ResponseStatus inherits String
         }
        
        service CsvConsumerApi {
           @taxi.http.HttpOperation(method = "POST", url = "http://localhost:${server.port}/csv")
           write operation saveCsv(@taxi.http.RequestBody CsvModel): RestResponse
        }
        
        @taxi.http.HttpOperation(method = "POST", url = "/api/q/csv")
        query CsvQuery(@taxi.http.RequestBody csvModel: CsvModel) {
           given { csvModel }
           call CsvConsumerApi::saveCsv
        }
      """.trimIndent())
               )
           )
       }


       @Bean
       @Primary
       fun vyneProvider( schemaProvider: SchemaProvider): VyneProvider {
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
       fun streamResultStreamProvider(): StreamResultStreamProvider  {
           val streamResultStreamProvider =  object: StreamResultStreamProvider {
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

           val queryEventConsumer = object: QueryEventConsumer {
               override fun handleEvent(event: QueryEvent) {

               }

               override fun recordResult(operation: OperationResult, queryId: String) {

               }

           }

           val historyEventConsumerProvider =  object: HistoryEventConsumerProvider {
               override fun createEventConsumer(queryId: String, schema: Schema): QueryEventConsumer {
                   return queryEventConsumer
               }

           }

          val  queryService = QueryService(
               SimpleSchemaProvider(schemaProvider.schema),
               vyneProvider,
              historyEventConsumerProvider,
               Jackson2ObjectMapperBuilder().build(),
               ActiveQueryMonitor(),
               QueryResponseFormatter(listOf(CsvFormatSpec), SimpleSchemaProvider(schemaProvider.schema))
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

