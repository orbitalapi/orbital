package com.orbitalhq.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.PackageMetadata
import com.orbitalhq.VersionedSource
import com.orbitalhq.VyneProvider
import com.orbitalhq.cockpit.core.WebSocketConfig
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.pipelines.StreamResultsWebsocketPublisher
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.runtime.core.dispatcher.local.StreamResultSubscriptionManager
import com.orbitalhq.query.runtime.core.gateway.QueryRouteService
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.SimpleVyneProvider
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.reactor.asFlux
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Sinks
import reactor.kotlin.test.test

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publisher.method=Local",
      "vyne.schema.consumer.method=Local",
      "spring.main.allow-bean-definition-overriding=true",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.telemetry.enabled=false",
   ]
)
@ActiveProfiles("test")
class SavedQueryEndpointIntegrationTest : DatabaseTest() {

   @MockBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager

   @MockBean
   lateinit var packagesService: PackageService

   @MockBean
   lateinit var schemaEditorService: SchemaEditorService

   @MockBean
   lateinit var streamSubscriptionManager: StreamResultSubscriptionManager


   @Autowired
   lateinit var queryRouteService: QueryRouteService


   @LocalServerPort
   val randomServerPort = 0

   object TestSchema {
      val source = """
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

            stream getNewReleases  : Stream<NewReleaseAnnouncement>
         }

         @HttpOperation(url = "/api/q/films", method = "GET")
         query GetFilms {
            find { Film[] }
         }
         @HttpOperation(url = "/api/q/newReleases", method = "GET")
         @WebsocketOperation(path = "/api/s/newReleases")
         query GetNewReleases {
            stream { NewReleaseAnnouncement }
         }


         @HttpOperation(url = "/api/wrongPath/newReleases", method = "GET")
         @WebsocketOperation(path = "/api/wrongPath/newReleases")
         query BadQuery {
            stream { NewReleaseAnnouncement }
         }
      }
      """.trimIndent()

      val schema = TaxiSchema.from(source, "UserSchema", "0.1.0")
   }

   @TestConfiguration
   @Import(TestDiscoveryClientConfig::class, WebSocketConfig::class, StreamResultsWebsocketPublisher::class)
   class SpringConfig {

      @MockBean
      lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

      @MockBean
      lateinit var configLoader: WorkspaceConfigLoader

      @MockBean
      lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider

      @Bean
      @Primary
      fun schemaProvider(): SchemaProvider = TestSchemaProvider.withBuiltInsAnd(TestSchema.schema)

      @Bean
      fun queryMetricsReporter(): QueryMetricsReporter = NoOpMetricsReporter

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
         return SimpleVyneProvider(vyne)
      }
   }


   @Test
   fun `can fetch saved request-response query over http endpoint`() {
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      val result = client.get().uri("/api/q/films")
         .retrieve()
         .bodyToMono<List<Map<String, Any>>>()
         .block()
      result.shouldHaveSize(3)
   }

   @Test
   fun `can stream saved request-response query over http endpoint using SSE`() {
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      val result = client.get().uri("/api/q/films")
         .accept(MediaType.TEXT_EVENT_STREAM)
         .retrieve()
         .bodyToFlux<Map<String, Any>>()
      result.test()
         .expectSubscription()
         .expectNextMatches { true }
         .expectNextMatches { true }
         .expectNextMatches { true }
         .expectComplete()
         .verify()
   }

   @Test
   fun `can stream streaming query over http endpoint using SSE`() {
      val sink = Sinks.many().unicast().onBackpressureBuffer<Any>()
      whenever(streamSubscriptionManager.getResultStream(any())).thenReturn(sink.asFlux())

      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      val result = client.get().uri("/api/q/newReleases")
         .accept(MediaType.TEXT_EVENT_STREAM)
         .retrieve()
         .bodyToFlux<Map<String, Any>>()
      result.test()
         .expectSubscription()
         .then { sink.emitNext(film("123", "Star Wars"), Sinks.EmitFailureHandler.FAIL_FAST) }
         .expectNextMatches { next -> next.hasTitle("Star Wars") }
         .then { sink.emitNext(film("123", "Empire Strikes Back"), Sinks.EmitFailureHandler.FAIL_FAST) }
         .expectNextMatches { next -> next.hasTitle("Empire Strikes Back") }
         .then { sink.tryEmitComplete() }
         .expectComplete()
         .verify()
   }

   @Test
   fun `can stream streaming query over websocket`() {
      val publisherSink = Sinks.many().unicast().onBackpressureBuffer<Any>()
      whenever(streamSubscriptionManager.getResultStream(any())).thenReturn(publisherSink.asFlux())
      val objectMapper = ObjectMapper()
      val client = HttpClient(CIO) {
         install(WebSockets)
      }

      runBlocking {
         client.webSocket("ws://localhost:$randomServerPort/api/s/newReleases") {
            incoming.receiveAsFlow()
               .asFlux()
               .test()
               .expectSubscription()
               .then {
                  publisherSink.emitNext(film("123", "Star Wars"), Sinks.EmitFailureHandler.FAIL_FAST)
               }
               .expectNextMatches { frame ->
                  frameAsMap(frame).hasTitle("Star Wars")
               }
               .then {
                  publisherSink.emitNext(film("123", "Empire Strikes Back"), Sinks.EmitFailureHandler.FAIL_FAST)
               }
               .expectNextMatches { frame ->
                  frameAsMap(frame).hasTitle("Empire Strikes Back")
               }
               .thenCancel()
               .verify()
         }
      }
   }

   @Test
   fun `requesting an incorrect url returns a 404`() {
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      val exception = assertThrows<NotFound> {
         val result = client.get().uri("/api/q/foo")
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block()
      }
      exception.statusCode.value().shouldBe(404)

   }

   @Test
   fun `query exposed with the wrong url prefix is not found`() {
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      val exception = assertThrows<NotFound> {
         val result = client.get().uri("/api/wrongPath/newReleases")
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block()
      }
      exception.statusCode.value().shouldBe(404)
   }

   @Test
   fun `websocket query exposed with the wrong url prefix is not found`() {
      val publisherSink = Sinks.many().unicast().onBackpressureBuffer<Any>()
      whenever(streamSubscriptionManager.getResultStream(any())).thenReturn(publisherSink.asFlux())

      val client = HttpClient(CIO) {
         install(WebSockets)
      }
      runBlocking {
         val exception = assertThrows<WebSocketException> {
            client.webSocket("ws://localhost:$randomServerPort/api/wrongPath/newReleases") {
               incoming.receiveAsFlow()
                  .asFlux()
                  .test()
            }
         }
         exception.message.shouldBe("Handshake exception, expected status code 101 but was 404")

      }
   }

   fun film(id: String, title: String) = mapOf("id" to id, "title" to title)
}

private fun Map<*, *>.hasTitle(title: String): Boolean = this["title"] == title

fun frameAsMap(frame: Frame, objectMapper: ObjectMapper = jacksonObjectMapper()): Map<String, Any> {
   val textFrame = frame.shouldBeInstanceOf<Frame.Text>()
   val map = objectMapper.readValue<Map<String, Any>>(textFrame.readText())
   return map
}
