package com.orbitalhq.queryService

import com.hazelcast.core.HazelcastInstance
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.AuthClaimType
import com.orbitalhq.PackageMetadata
import com.orbitalhq.UserType
import com.orbitalhq.VersionedSource
import com.orbitalhq.VyneProvider
import com.orbitalhq.cockpit.core.WebSocketConfig
import com.orbitalhq.cockpit.core.pipelines.StreamResultsWebsocketPublisher
import com.orbitalhq.cockpit.core.security.authorisation.VyneAuthorisationConfig
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.pipelines.jet.streams.ResultStreamAuthorizationDecorator
import com.orbitalhq.pipelines.jet.streams.StreamResultsService
import com.orbitalhq.query.runtime.core.gateway.QueryRouteService
import com.orbitalhq.queryService.security.TestRoles.adminUserName
import com.orbitalhq.queryService.security.TestRoles.platformManagerUser
import com.orbitalhq.queryService.security.TestRoles.viewerUserName
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.SimpleVyneProvider
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import com.orbitalhq.testVyne
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.runner.RunWith
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Sinks
import reactor.kotlin.test.test
import java.time.Duration

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

      "vyne.schema.publisher.method=Local",
      "vyne.schema.consumer.method=Local",
      "spring.main.allow-bean-definition-overriding=true",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.security.open-idp.jwks-uri=\${wiremock.server.baseUrl}/.well-known/jwks.json",
      "vyne.security.openIdp.enabled=true",
      "vyne.security.open-idp.issuer-url=http://localhost:\${wiremock.server.port}",
      "vyne.security.open-idp.client-id=vyne-spa",
      "wiremock.server.baseUrl=http://localhost:\${wiremock.server.port}",
      "logging.level.org.springframework.security=DEBUG",
      "vyne.analytics.persistResults=true",
      "vyne.telemetry.enabled=false",
   ]
)
class SavedQueryWithAuthPolicyIntegrationTest : BaseIntegrationTest() {

   object TestSchema {
      val source = """
         ${AuthClaimType.AuthClaimsTypeDefinition}
         ${UserType.UsernameTypeDefinition}
         ${ErrorType.ErrorTypeDefinition}
      namespace com.petflix {
         model Film {
            filmId : FilmId inherits Int
            title : Title inherits String
         }
         model NewReleaseAnnouncement {
            filmId : FilmId
         }
         service FilmsService {
            operation getFilms() : Film[]
            operation getSingleFilm(): Film
            stream getNewReleases  : Stream<NewReleaseAnnouncement>
         }

         @HttpOperation(url = "/api/q/films", method = "GET")
         query GetFilms {
            find { Film[] }
         }

         @HttpOperation(url = "/api/q/films/123", method = "GET")
         query GetSingleFilm {
            find { Film }
         }

         @HttpOperation(url = "/api/q/newReleases", method = "GET")
         @WebsocketOperation(path = "/api/s/newReleases")
         query GetNewReleases {
            stream { Film }
         }

         @HttpOperation(url = "/api/wrongPath/newReleases", method = "GET")
         @WebsocketOperation(path = "/api/wrongPath/newReleases")
         query BadQuery {
            stream { NewReleaseAnnouncement }
         }

         type Role inherits String
         model UserInfo inherits ${AuthClaimType.AuthClaimsTypeName.parameterizedName}{
            realm_access : {
               roles : Role[]
            }
         }

         policy HideTitle against Film (userInfo:UserInfo) -> {
            read {
               when {
                  userInfo.realm_access.roles.contains("Admin") -> Film
                  userInfo.realm_access.roles.contains("Viewer") -> throw( (NotAuthorizedError) { message: 'Not Authorized' })
                  else -> Film as { ... except { title } }
               }
            }
         }
      }
      """.trimIndent()

      val schema = TaxiSchema.from(source, "UserSchema", "0.1.0")
   }

   @TestConfiguration
   @Import(TestDiscoveryClientConfig::class, WebSocketConfig::class, StreamResultsWebsocketPublisher::class, StreamResultsService::class, ResultStreamAuthorizationDecorator::class)
   class SpringConfig {

      @Bean
      @Primary
      fun schemaProvider(): SchemaProvider = TestSchemaProvider.withBuiltInsAnd(TestSchema.schema)

      @Bean
      @Primary
      fun queryMetricsReporter(): QueryMetricsReporter = NoOpMetricsReporter

      @Bean
      fun hazelcastInstance(): HazelcastInstance = MockHazelcastInstance()

      @Bean
      fun schemaStore(): LocalValidatingSchemaStoreClient {
         val schemaStore = LocalValidatingSchemaStoreClient()
         schemaStore.submitSchemas(
            PackageMetadata.from("com.foo", "test", "1.0.0"),
            listOf(VersionedSource.sourceOnly(TestSchema.source))
         )
         return schemaStore
      }

      @Bean
      @Primary
      fun vyneProvider(schemaStore: SchemaStore): VyneProvider {
         val (vyne, stub) = testVyne(TestSchema.source)
         stub.addResponse(
            "getFilms", vyne.parseJson(
               "com.petflix.Film[]", """
            [
               { "filmId": "1010", "title": "Star Wars" },
               { "filmId": "1020", "title": "Empire Strikes Back" },
               { "filmId": "1030", "title": "Return of the Jedi" }
            ]
         """.trimIndent()
            )
         )
         stub.addResponse("getSingleFilm", vyne.parseJson(
            "com.petflix.Film", """
               { "filmId": "1010", "title": "Star Wars" }
            """.trimIndent()
         ))
         return SimpleVyneProvider(vyne)
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

   @Value("\${wiremock.server.baseUrl}")
   private lateinit var wireMockServerBaseUrl: String

   @Autowired
   lateinit var queryRouteService: QueryRouteService

   @Test
   fun `requesting a saved request-response query applies policy hiding value`() {
      val result = loadUrlResponseForUser(platformManagerUser, "/api/q/films")
      result.shouldContainAll(
         mapOf("filmId" to 1010, "title" to null),
         mapOf("filmId" to 1020, "title" to null),
         mapOf("filmId" to 1030, "title" to null),
      )
   }

   @Test
   fun `requesting a saved request-response query applies policy permitting value`() {
      val result = loadUrlResponseForUser(adminUserName, "/api/q/films")
      result.shouldContainAll(
         mapOf("filmId" to 1010, "title" to "Star Wars"),
         mapOf("filmId" to 1020, "title" to "Empire Strikes Back"),
         mapOf("filmId" to 1030, "title" to "Return of the Jedi"),
      )
   }
   @Test
   fun `requesting a saved request-response single-value query applies policy hiding value`() {
      val result = loadSingleValueUrlResponseForUser(platformManagerUser, "/api/q/films/123")
      result.shouldBe(
         mapOf("filmId" to 1010, "title" to null),
      )
   }

   @Test
   fun `requesting a saved request-response single-value query applies policy permitting value`() {
      val result = loadSingleValueUrlResponseForUser(adminUserName, "/api/q/films/123")
      result.shouldBe(
         mapOf("filmId" to 1010, "title" to "Star Wars"),
      )
   }

   @Test
   fun `requesting a saved request-response stream applies policy permitting value`() {
      val result = loadUrlStreamForUser(adminUserName, "/api/q/films", count = 3)
      result.shouldContainAll(
         mapOf("filmId" to 1010, "title" to "Star Wars"),
         mapOf("filmId" to 1020, "title" to "Empire Strikes Back"),
         mapOf("filmId" to 1030, "title" to "Return of the Jedi"),
      )
   }

   @Test
   fun `requesting a saved request-response stream applies policy hiding value`() {
      val result = loadUrlStreamForUser(platformManagerUser, "/api/q/films", count = 3)
      result.shouldContainAll(
         mapOf("filmId" to 1010, "title" to null),
         mapOf("filmId" to 1020, "title" to null),
         mapOf("filmId" to 1030, "title" to null),
      )
   }

   @Test
   fun `requesting a saved request-response query applies policy returning 401 Not Authorized`() {
      val (status,body) = getUrlResponseSpecForUser(viewerUserName, "/api/q/films")
         .statusCodeAndBody()
      status.value().shouldBe(401)
      body.shouldBe("Not Authorized")
   }

   @Test
   fun `requesting a saved request-response with single return value query applies policy returning 401 Not Authorized`() {
      val (status,body) = getUrlResponseSpecForUser(viewerUserName, "/api/q/films/123")
         .statusCodeAndBody()
      status.value().shouldBe(401)
      body.shouldBe("Not Authorized")
   }

   @Test
   fun `requesting a saved request-response SSE stream applies policy returning 401 Not Authorized`() {
      val (status, body) = getUrlStreamResponseSpecForUser(viewerUserName, "/api/q/films")
         .statusCodeAndBody()
      status.value().shouldBe(401)
      body.shouldBe("Not Authorized")
   }

   @Test
   fun `observe a persistent stream over SSE with a policy applied permitting value`() {
      val sink = Sinks.many().unicast().onBackpressureBuffer<Any>()
      whenever(hazelcastStreamObserver.getResultStream(any())).thenReturn(sink.asFlux())

      val result = getUrlStreamResponseSpecForUser(adminUserName, "/api/q/newReleases")
         .retrieve()
         .bodyToFlux<Map<String,Any?>>()

      result.test()
         .expectSubscription()
         .then { sink.emitNext(film(123, "Star Wars"), Sinks.EmitFailureHandler.FAIL_FAST) }
         .expectNextMatches { next -> next.hasTitle("Star Wars") }
         .then { sink.emitNext(film(123, "Empire Strikes Back"), Sinks.EmitFailureHandler.FAIL_FAST) }
         .expectNextMatches { next -> next.hasTitle("Empire Strikes Back") }
         .then { sink.tryEmitComplete() }
         .expectComplete()
         .verify()
   }

   @Test
   fun `observe a persistent stream over SSE with a policy applied hiding a value`() {
      val sink = Sinks.many().unicast().onBackpressureBuffer<Any>()
      whenever(hazelcastStreamObserver.getResultStream(any(),)).thenReturn(sink.asFlux())

      val result = getUrlStreamResponseSpecForUser(platformManagerUser, "/api/q/newReleases")
         .retrieve()
         .bodyToFlux<Map<String,Any?>>()

      result.test()
         .expectSubscription()
         .then { sink.emitNext(film(123, "Star Wars"), Sinks.EmitFailureHandler.FAIL_FAST) }
         .expectNextMatches { next -> next.hasTitle(null) }
         .then { sink.emitNext(film(123, "Empire Strikes Back"), Sinks.EmitFailureHandler.FAIL_FAST) }
         .expectNextMatches { next -> next.hasTitle(null) }
         .then { sink.tryEmitComplete() }
         .expectComplete()
         .verify()
   }

   private fun film(id: Int, title: String) = mapOf("filmId" to id, "title" to title)

   private fun Map<*, *>.hasTitle(title: String?): Boolean = this["title"] == title

   private fun getUrlResponseSpecForUser(username: String, url: String): WebClient.RequestHeadersSpec<*> {
      val token = getTestAuthToken(username)
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      return client.get().uri(url)
         .header("Authorization", "Bearer $token")
   }

   private fun getUrlStreamResponseSpecForUser(username: String, url: String): WebClient.RequestHeadersSpec<*> {
      val token = getTestAuthToken(username)
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      return client.get().uri(url)
         .header("Authorization", "Bearer $token")
         .accept(MediaType.TEXT_EVENT_STREAM)
   }

   private fun loadUrlResponseForUser(username: String, url: String): List<Map<String, Any>> {
      return getUrlResponseSpecForUser(username, url)
         .retrieve()
         .bodyToMono<List<Map<String, Any>>>()
         .block()!!
   }

   private fun loadSingleValueUrlResponseForUser(username: String, url: String): Map<String, Any> {
      return getUrlResponseSpecForUser(username, url)
         .retrieve()
         .bodyToMono<Map<String, Any>>()
         .block()!!
   }

   private fun loadUrlStreamForUser(username: String, url: String, count: Int): List<Map<String, Any>> {
      val token = getTestAuthToken(username)
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      return client.get().uri(url)
         .header("Authorization", "Bearer $token")
         .accept(MediaType.TEXT_EVENT_STREAM)
         .retrieve()
         .bodyToFlux<Map<String, Any>>()
         .take(count.toLong())
         .collectList()
         .block(Duration.ofSeconds(2))!!
   }

   private fun getTestAuthToken(username: String) =
      com.orbitalhq.queryService.security.getTestAuthToken(username, wireMockServerBaseUrl)

   private fun getTestAuthToken(roles: List<String>) {
      com.orbitalhq.queryService.security.getTestAuthToken(
         "adminUser", wireMockServerBaseUrl, mapOf(
            "roles" to roles
         )
      )
   }


}

private fun WebClient.RequestHeadersSpec<*>.statusCodeAndBody(): Pair<HttpStatusCode, String> {
   return this.exchangeToMono { response ->
      response.bodyToMono<String>().defaultIfEmpty("")
         .map { response.statusCode() to it }
   }
      .block()!!

}
