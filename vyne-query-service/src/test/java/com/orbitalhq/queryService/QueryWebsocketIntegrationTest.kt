package com.orbitalhq.queryService

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.PackageMetadata
import com.orbitalhq.VersionedSource
import com.orbitalhq.VyneProvider
import com.orbitalhq.cockpit.core.ConfigService
import com.orbitalhq.cockpit.core.WebSocketConfig
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.pipelines.StreamResultsWebsocketPublisher
import com.orbitalhq.copilot.OpenAiChatService
import com.orbitalhq.licensing.LicenseManager
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.runtime.core.WebsocketQuery
import com.orbitalhq.query.runtime.core.dispatcher.local.RSocketStreamResultSubscriptionManager
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
import com.orbitalhq.utils.Ids
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
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
class QueryWebsocketIntegrationTest : DatabaseTest() {

   @MockBean
   lateinit var chatService: OpenAiChatService

   @MockBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager

   @MockBean
   lateinit var packagesService: PackageService

   @MockBean
   lateinit var schemaEditorService: SchemaEditorService

   @MockBean
   lateinit var streamSubscriptionManager: RSocketStreamResultSubscriptionManager

   @Autowired
   lateinit var resultsSink: Sinks.Many<String>

   @MockBean
   lateinit var configService: ConfigService
   @MockBean
   lateinit var licenseManager: LicenseManager

   @MockBean
   lateinit var hazelcastInstance: HazelcastInstance



   @LocalServerPort
   val randomServerPort = 0

   object TestSchema {
      val source = """
      namespace com.petflix {
         model NewReleaseAnnouncement {
            filmId : FilmId inherits Int
         }
         service FilmsService {
            stream getNewReleases  : Stream<NewReleaseAnnouncement>
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
      fun responseFlow(): Sinks.Many<String> {
         return Sinks.many().unicast().onBackpressureBuffer<String>()
      }

      @Bean
      @Primary
      fun vyneProvider(schemaStore: SchemaStore, responseFlow: Sinks.Many<String>): VyneProvider {
         val (vyne, stub) = testVyne(TestSchema.source)
         stub.addResponseFlow("getNewReleases") { _, _ ->
            responseFlow.asFlux()
               .map { vyne.parseJson("NewReleaseAnnouncement", it) }
               .asFlow()
         }
         return SimpleVyneProvider(vyne)
      }
   }

   @Test
   fun `when the client cancels a query no more results are emitted on the websocket`() {
      val client = HttpClient(CIO) {
         install(WebSockets)
      }
      val clientId = Ids.id(prefix = "query")
      val query = jacksonObjectMapper().writeValueAsString(
         WebsocketQuery(
            clientQueryId = clientId,
            query = "stream { NewReleaseAnnouncement }"
         )
      )

      fun Map<*, *>.mapAt(key: String): Map<String, Any> {
         return this[key] as Map<String, Any>
      }
      runBlocking {
         client.webSocket("ws://localhost:$randomServerPort/api/query/taxiql") {
            send(query)
            incoming.receiveAsFlow()
               .asFlux()
               .test()
               .expectSubscription()
               .then { resultsSink.tryEmitNext("""{ "filmId" : 123 }""") }
               .expectNextMatches { frame ->
                  val map = frameAsMap(frame)
                  map.mapAt("value")["filmId"].shouldBe(123)
                  true
               }
               .then { resultsSink.tryEmitNext("""{ "filmId" : 456 }""") }
               .expectNextMatches { frame ->
                  val map = frameAsMap(frame)
                  map.mapAt("value")["filmId"].shouldBe(456)
                  true
               }
               .then {
                  runBlocking {
                     client.delete("http://localhost:$randomServerPort/api/query/active/$clientId")
                  }
               }
               .then { resultsSink.tryEmitNext("""{ "filmId" : 456 }""") }
               .expectComplete()
               .verify()
         }
      }
   }
}
