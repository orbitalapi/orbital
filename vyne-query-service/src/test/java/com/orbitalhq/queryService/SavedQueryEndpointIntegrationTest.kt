package com.orbitalhq.queryService

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hazelcast.core.HazelcastInstance
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.Vyne
import com.orbitalhq.VyneProvider
import com.orbitalhq.cockpit.core.ConfigService
import com.orbitalhq.cockpit.core.WebSocketConfig
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.content.DefaultContentRepository
import com.orbitalhq.cockpit.core.pipelines.StreamResultsWebsocketPublisher
import com.orbitalhq.copilot.CopilotConversationApi
import com.orbitalhq.licensing.OrbitalLicenseManager
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.pipelines.jet.streams.HazelcastStreamResultObserver
import com.orbitalhq.pipelines.jet.streams.ResultStreamAuthorizationDecorator
import com.orbitalhq.pipelines.jet.streams.StreamResultsService
import com.orbitalhq.query.Fact
import com.orbitalhq.query.runtime.core.gateway.QueryRouteService
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schema.consumer.SchemaStoreToSchemaProviderWrapper
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import io.kotest.assertions.timing.eventually
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.reactor.asFlux
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.kotlin.test.test
import kotlin.time.Duration

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

   @MockitoBean
   lateinit var cmsService: DefaultContentRepository


   @MockitoBean
   lateinit var chatService: CopilotConversationApi

   @MockitoBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager

   @MockitoBean
   lateinit var packagesService: PackageService

   @MockitoBean
   lateinit var schemaEditorService: SchemaEditorService

   @Autowired
   lateinit var streamSubscriptionManager: StreamResultsService

   @MockitoBean
   lateinit var hazelcastStreamResultObserver: HazelcastStreamResultObserver

   @MockitoBean
   lateinit var configService: ConfigService

   @MockitoBean
   lateinit var licenseManager: OrbitalLicenseManager

   @MockitoBean
   lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

   @MockitoBean
   lateinit var configLoader: WorkspaceConfigLoader

   @MockitoBean
   lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider

   @Autowired
   lateinit var queryRouteService: QueryRouteService

   @Autowired
   lateinit var vyneProvider: SimpleVyneAndStubFactory

   @Autowired
   lateinit var schemaStore: LocalValidatingSchemaStoreClient


   @LocalServerPort
   val randomServerPort = 0

   object TestSchema {
      /**
       * STOP! Before adding to this ever-growing "one schema to rule all the tests,
       * consider defining a schema that covers just your test case scenario.
       *
       * See:
       *  - fails with helpful message if request body annotation is missing
       *  - can call map to invoke multiple mutations and return result
       *
       *  As examples of tests using the improved sendToApi<T> helper method.
       */
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

         @HttpOperation(url = "/api/q/films", method = "POST")
         query EchoFilm(@RequestBody film : Film) {
            given { film }
            find { Film }
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
      fun schemaProvider(store: SchemaStore): SchemaProvider = SchemaStoreToSchemaProviderWrapper(store)

      @Bean
      fun hazelcastInstance(): HazelcastInstance = MockHazelcastInstance()


      @Bean
      fun queryMetricsReporter(): QueryMetricsReporter = NoOpMetricsReporter

      @Bean
      fun schemaStore(): LocalValidatingSchemaStoreClient {
         val schemaStore = LocalValidatingSchemaStoreClient()
//         schemaStore.submitSchemas(
//            PackageMetadata.from("com.foo", "test", "1.0.0"),
//            listOf(VersionedSource.sourceOnly(TestSchema.source))
//         )
         return schemaStore
      }

      @Bean
      @Primary
      fun vyneProvider(schemaStore: SchemaStore): VyneProvider {
         val factory = SimpleVyneAndStubFactory(schemaStore)
         return factory
      }
   }


   @Test
   fun `calling a post endpoint without a valid request body returns bad request`() {
      useDefaultStubAndSchema()
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      val result = client.post().uri("/api/q/films")
         .exchangeToMono { it -> Mono.just(it) }
         .block()!!
      result.statusCode().value().shouldBe(400)
   }


   @Test
   fun `can fetch saved request-response query over http endpoint`() {
      useDefaultStubAndSchema()
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      val result = client.get().uri("/api/q/films")
         .retrieve()
         .bodyToMono<List<Map<String, Any>>>()
         .block()!!
      result.shouldHaveSize(3)
   }

   @Test
   fun `can stream saved request-response query over http endpoint using SSE`() {
      useDefaultStubAndSchema()
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
      useDefaultStubAndSchema()
      val sink = Sinks.many().unicast().onBackpressureBuffer<Any>()
      whenever(hazelcastStreamResultObserver.getResultStream(any())).thenReturn(sink.asFlux())

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
      useDefaultStubAndSchema()
      val publisherSink = Sinks.many().unicast().onBackpressureBuffer<Any>()
      whenever(hazelcastStreamResultObserver.getResultStream(any())).thenReturn(publisherSink.asFlux())
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
      useDefaultStubAndSchema()
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
      useDefaultStubAndSchema()
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


   private inline fun <reified T> sendToApi(
      path: String,
      method: HttpMethod,
      body: String,
      headers: Map<String, String> = emptyMap()
   ): T {
      val client = WebClient.builder()
         .baseUrl("http://localhost:$randomServerPort")
         .build()
      return client.method(method)
         .uri(path)
         .headers { h ->
            headers.forEach { header, value ->
               h.add(header, value)
            }
         }
         .bodyValue(body)
         .retrieve()
         .onStatus({ it.is5xxServerError }, { response ->
            response.bodyToMono<String>()
               .map { body ->
                  RuntimeException("The request failed with ${response.statusCode()} - $body}")
               }
         })
         .bodyToMono(T::class.java)
         .block()!!
   }

   @Test
   fun `fails with helpful message if request body annotation is missing`() {
      val schema = """
         model Person {
            name :  PersonName inherits String
         }
         @HttpOperation(url = "/api/q/person", method = "POST")
         query EchoPerson(person:Person) {
            find { Person }
         }
      """.trimIndent()
      submitSchemaAndFetchStub(schema, routeToWaitFor = "/api/q/person" to HttpMethod.POST)
      val exception = assertThrows<WebClientResponseException.BadRequest> {
         sendToApi<Map<String, Any>>("/api/q/person", HttpMethod.POST, """{ "name" : "Jimmy" }""".trimMargin())
      }
      exception.getResponseBodyAsString(Charsets.UTF_8)
         .shouldBe("Parameter 'person' needs an annotation to specify how it should be resolved from the request. Consider adding one of taxi.http.HttpHeader, taxi.http.RequestBody, taxi.http.QueryVariable, taxi.http.PathVariable. (Check imports if annotation seems present but isn't recognized).")
   }

   @Test
   fun `can serve csv from endpoint based on annotation`() {
      val schema = """
         model Person {
            name :  PersonName inherits String
         }
         service PersonApi {
            operation getAll():Person[]
         }
         @com.orbitalhq.formats.Csv
         model PersonCsv {
            called : PersonName
            also: String = "another value"
         }
         @HttpOperation(url = "/api/q/person", method = "GET")
         query FindPeople {
            find { Person[] } as PersonCsv[]
         }
      """.trimIndent()
      val stub = submitSchemaAndFetchStub(schema, routeToWaitFor = "/api/q/person" to HttpMethod.GET)
      stub.addResponse("getAll", """[ { "name" : "Jimmy"}, { "name" : "Jack" } ]""")
      val response = sendToApi<String>("/api/q/person", HttpMethod.GET, "")
      val CR = "\r"
      val expected = """called,also$CR
Jimmy,another value$CR
Jack,another value$CR
"""
      response.shouldBe(expected)
   }

   @Test
   fun `can serve single csv from endpoint based on annotation`() {
      val schema = """
         model Person {
            name :  PersonName inherits String
         }
         service PersonApi {
            operation getAll():Person
         }
         @com.orbitalhq.formats.Csv
         model PersonCsv {
            called : PersonName
            also: String = "another value"
         }
         @HttpOperation(url = "/api/q/person", method = "GET")
         query FindPeople {
            find { Person } as PersonCsv
         }
      """.trimIndent()
      val stub = submitSchemaAndFetchStub(schema, routeToWaitFor = "/api/q/person" to HttpMethod.GET)
      stub.addResponse("getAll", """{ "name" : "Jimmy"}""")
      val response = sendToApi<String>("/api/q/person", HttpMethod.GET, "")
      val CR = "\r"
      val expected = """called,also$CR
Jimmy,another value$CR
"""
      response.shouldBe(expected)
   }

   @Test
   fun `can serve csv from endpoint based on inline annotation`() {
      val schema = """
         model Person {
            name :  PersonName inherits String
         }
         service PersonApi {
            operation getAll():Person[]
         }

         @HttpOperation(url = "/api/q/person", method = "GET")
         query FindPeople {
            find { Person[] } as
            @com.orbitalhq.formats.Csv
            {
               called : PersonName
               also: String = "another value"
            }[]
         }
      """.trimIndent()
      val stub = submitSchemaAndFetchStub(schema, routeToWaitFor = "/api/q/person" to HttpMethod.GET)
      stub.addResponse("getAll", """[ { "name" : "Jimmy"}, { "name" : "Jack" } ]""")
      val response = sendToApi<String>("/api/q/person", HttpMethod.GET, "")
      val CR = "\r"
      val expected = """called,also$CR
Jimmy,another value$CR
Jack,another value$CR
"""
      response.shouldBe(expected)
   }

   @Test
   fun `can serve csv from endpoint based on inheritence with annotation`() {
      val schema = """
         model Person {
            name :  PersonName inherits String
         }
         service PersonApi {
            operation getAll():Person[]
         }

         @com.orbitalhq.formats.Csv
         model CsvPerson inherits Person {
            also: String = "another value"
         }

         @HttpOperation(url = "/api/q/person", method = "GET")
         query FindPeople {
            find { Person[] } as CsvPerson[]
         }
      """.trimIndent()
      val stub = submitSchemaAndFetchStub(schema, routeToWaitFor = "/api/q/person" to HttpMethod.GET)
      stub.addResponse("getAll", """[ { "name" : "Jimmy"}, { "name" : "Jack" } ]""")
      val response = sendToApi<String>("/api/q/person", HttpMethod.GET, "")
      val CR = "\r"
      val expected = """name,also$CR
Jimmy,another value$CR
Jack,another value$CR
"""
      response.shouldBe(expected)
   }


   @Test
   fun `can call map to invoke multiple mutations and return result`() {
      val schema = """
          type ApiKey inherits String

         @com.orbitalhq.formats.Csv
         model Person {
            name :  PersonName inherits String
            points : Points inherits Int
            score : Score inherits Int
         }
         parameter model PersonUpdate {
            called : PersonName
            newPoints : NewPoints
         }
         // This is to ensure that projection is happening correctly, and that
         // evals are performed with the correct scope
         type NewPoints inherits Int = (Points,Score) -> Points + Score
         service PersonApi {
            write operation saveOne( PersonName, ApiKey, PersonUpdate ) : PersonUpdate
         }
         @HttpOperation(url = "/api/q/people", method = "POST")
         query UpdateTheCsvPeople(@taxi.http.RequestBody people:Person[]) {
            given { ApiKey = '123', people }
            map { Person }
            call PersonApi::saveOne
         }
      """.trimIndent()
      val stub = submitSchemaAndFetchStub(schema, routeToWaitFor = "/api/q/people" to HttpMethod.POST)
      stub.addResponseReturningInputs("saveOne")
      val response = sendToApi<List<Map<String, Any>>>(
         "/api/q/people", HttpMethod.POST, """name,points,score
         |jimmy,2,4
         |jack,5,7""".trimMargin()
      )
      response.toSet().shouldBe(
         setOf(
            mapOf("called" to "jimmy", "newPoints" to 6),
            mapOf("called" to "jack", "newPoints" to 12),
         )
      )
   }

   fun useDefaultStubAndSchema() {
      val stub = submitSchemaAndFetchStub(TestSchema.source, routeToWaitFor = "/api/q/films" to HttpMethod.GET)
      stub!!.addResponse(
         "getFilms", """
            [
               { "filmId": "1010", "title": "Star Wars" },
               { "filmId": "1020", "title": "Empire Strikes Back" },
               { "filmId": "1030", "title": "Return of the Jedi" }
            ]
         """.trimIndent()
      )
   }

   private fun submitSchemaAndFetchStub(schema: String, routeToWaitFor: Pair<String, HttpMethod>): StubService {
      val submissionResult = schemaStore.submitSchemas(
         PackageMetadata.from("com.orbital", "test"),
         listOf(VersionedSource.sourceOnly(schema))
      )
      submissionResult.getOrElse { exception ->
         fail("There were schema compilation errors", exception)
      }

      runBlocking {
         eventually(Duration.parse("10s")) {
            queryRouteService.findRoute(routeToWaitFor.first, routeToWaitFor.second)
               .shouldNotBeNull()
         }
      }
      val (vyne, stub) = vyneProvider.buildVyneFromSchemaStore()
      return stub
   }

   @Test
   fun `can pass http header value to query`() {
      val schema = """
         @HttpOperation(url = "/api/q/hello", method = "GET")
         query UpdateTheCsvPeople(
            @taxi.http.HttpHeader(name = "X-Request-Name") personName:String
         ) {
            find {
               message : String = "Hello " + personName
            }
         }

      """.trimIndent()
      submitSchemaAndFetchStub(schema, routeToWaitFor = "/api/q/hello" to HttpMethod.GET)
      val response = sendToApi<String>(
         "/api/q/hello", HttpMethod.GET, "", headers = mapOf(
            "X-Request-Name" to "Jimmy"
         )
      )
      val responseMap = jacksonObjectMapper().readValue<Map<String, String>>(response)
      responseMap["message"].shouldBe("Hello Jimmy")
   }

   @Test
   fun `websocket query exposed with the wrong url prefix is not found`() {
      val publisherSink = Sinks.many().unicast().onBackpressureBuffer<Any>()
      whenever(hazelcastStreamResultObserver.getResultStream(any())).thenReturn(publisherSink.asFlux())

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

/**
 * Implementation of VyneProvider.
 * Unlike our normal SimpleVyneProvider, this one allows the
 * schema to change during test - which is helpful when wanting to add / remove
 * routes after the spring context has been built.
 */
class SimpleVyneAndStubFactory(private val schemaStore: SchemaStore) : VyneProvider {
   fun buildVyneFromSchemaStore(): Pair<Vyne, StubService> {
      val (vyne, stub) = testVyne(schemaStore.schema().asTaxiSchema(), formatSpecs = FormatSpecRegistry.DEFAULT_SPECS)
      this.stub = stub
      this.vyne = vyne;
      return vyne to stub
   }

   var stub: StubService? = null
      private set;
   var vyne: Vyne? = null
      private set;


   override fun createVyne(facts: Set<Fact>): Vyne {
      if (vyne == null) {
         buildVyneFromSchemaStore()
      }
      return this.vyne!!
   }

   override fun createVyne(
      facts: Set<Fact>,
      schema: Schema,
      queryOptions: QueryOptions
   ): Vyne {
      if (vyne == null) {
         buildVyneFromSchemaStore()
      }
      return this.vyne!!
//      val (vyne,stub) = testVyne(schema.asTaxiSchema())
//      this.vyne = vyne
//      this.stub = stub
//      return this.vyne!!
   }

}
