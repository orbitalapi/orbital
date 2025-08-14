package com.orbitalhq.queryService

import com.hazelcast.core.HazelcastInstance
import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.Duration
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.cockpit.core.WebSocketConfig
import com.orbitalhq.cockpit.core.pipelines.StreamResultsWebsocketPublisher
import com.orbitalhq.cockpit.core.security.authorisation.VyneAuthorisationConfig
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.pipelines.jet.streams.ResultStreamAuthorizationDecorator
import com.orbitalhq.pipelines.jet.streams.StreamResultsService
import com.orbitalhq.query.runtime.core.gateway.QueryRouteService
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@RunWith(SpringRunner::class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "server.error.include-message=always",
      "server.error.include-exception=true",
      "server.error.include-stacktrace=always",
      "server.error.include-binding-errors=always",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.telemetry.enabled=false",
   ]
)
class SavedQueryWithValidationErrorsIntegrationTest : BaseIntegrationTest() {

   @TestConfiguration
   @Import(
      TestDiscoveryClientConfig::class,
      WebSocketConfig::class,
      StreamResultsWebsocketPublisher::class,
      StreamResultsService::class,
      ResultStreamAuthorizationDecorator::class
   )
   class SpringConfig {

      @Bean
      @Primary
      fun queryMetricsReporter(): QueryMetricsReporter = NoOpMetricsReporter

      @Bean
      fun hazelcastInstance(): HazelcastInstance = MockHazelcastInstance()

      @Bean
      @Primary
      fun schemaStore(): LocalValidatingSchemaStoreClient {
         val schemaStore = LocalValidatingSchemaStoreClient()
         return schemaStore
      }

      @Bean
      @Primary
      fun vyneProvider(schemaStore: SchemaStore): SimpleVyneAndStubFactory {
         return SimpleVyneAndStubFactory(schemaStore)
      }

      @Primary
      @Bean
      fun vyneAuthorisationConfig(): VyneAuthorisationConfig {
         return VyneAuthorisationConfig()
      }
   }

   @LocalServerPort
   val randomServerPort = 0

   @Autowired
   private lateinit var restTemplate: TestRestTemplate

   @Autowired
   lateinit var queryRouteService: QueryRouteService

   @Autowired
   lateinit var vyneProvider: SimpleVyneAndStubFactory

   @Autowired
   lateinit var schemaStore: LocalValidatingSchemaStoreClient

   @Test
   fun `can use a function to throw an error in the given clause and serve a custom response`() {
      submitSchemaAndWait("""
         import com.orbitalhq.errors.Error

         @taxi.http.ResponseBody
         @taxi.http.ResponseCode(400)
         model BadRequestError inherits Error {
           Code: String
           requestId : RequestId inherits String
         }

         function badRequest(requestId: RequestId):Nothing -> throw((BadRequestError) {
             Code: 'Nope',
             requestId: requestId
         })

         @HttpOperation(url = "/api/q/hello", method = "GET")
         query JustThrowErrors {
            given {
               badRequest : Any = badRequest("request-123")
            }
            find { "Hello" }
         }

      """.trimIndent())

      val webResponse = webClient().get().uri("/api/q/hello")
         .invoke<Map<String,Any>>()
      webResponse.statusCode.is2xxSuccessful.shouldBeFalse()
      webResponse.statusCode.shouldBe(HttpStatusCode.valueOf(400))
      webResponse.body.shouldBe(
         mapOf("Code" to "Nope",
            "requestId" to "request-123",
            ),
         )
   }

   private fun submitSchemaAndWait(schema: String) {
      val result = schemaStore.submitPackage(asSourcePackage(schema))
      result.isRight().shouldBeTrue()
      Awaitility.await().atMost(Duration.FIVE_SECONDS).until<Boolean> {
         queryRouteService.routes.size == 1
      }
   }

   private fun webClient(): WebClient {
      return WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
   }


   private fun asSourcePackage(source: String): SourcePackage {
      return SourcePackage(
         PackageMetadata.from("com.foo", "test"), listOf(
            VersionedSource.sourceOnly(source)
         )
      )
   }
}

inline fun <reified T> WebClient.RequestHeadersSpec<*>.invoke(): ResponseEntity<T> {
   return this.retrieve()
      .onStatus(HttpStatusCode::isError) { response -> Mono.empty() }
      .toEntity(T::class.java)
      .block()!!
}


