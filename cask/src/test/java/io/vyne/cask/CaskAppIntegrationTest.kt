package io.vyne.cask

import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.ingest.TestSchema.schemaWithConcatAndDefaultSource
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.query.generators.OperationGeneratorConfig
import io.vyne.cask.query.vyneql.VyneQlQueryService
import io.vyne.cask.services.CaskServiceBootstrap
import io.vyne.cask.services.DefaultCaskTypeProvider
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.junit.After
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.HandshakeInfo
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Mono
import reactor.netty.http.websocket.WebsocketInbound
import reactor.netty.http.websocket.WebsocketOutbound
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.BiFunction
import java.util.function.Consumer
import javax.sql.DataSource


@Testcontainers
@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.schema.publicationMethod=LOCAL"
   ]
)
@ActiveProfiles("test")
@EnableConfigurationProperties(OperationGeneratorConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CaskAppIntegrationTest {
   @LocalServerPort
   val randomServerPort = 0

   @Autowired
   lateinit var schemaPublisher: SchemaPublisher

   @Autowired
   lateinit var caskServiceBootstrap: CaskServiceBootstrap

   @Autowired
   lateinit var webTestClient: WebTestClient

   @Autowired
   lateinit var caskDao: CaskDAO

   @Autowired
   lateinit var configRepository: CaskConfigRepository

   @Autowired
   lateinit var caskService: CaskService

   @Autowired
   lateinit var schemaProvider: SchemaProvider

   @Autowired
   lateinit var schemaStoreClient: SchemaStoreClient


   @After
   fun tearDown() {
      val caskConfigs = configRepository.findAll()
      if (caskConfigs.isNotEmpty()) {
         val generation = schemaStoreClient.generation
         caskService.deleteCasks(caskConfigs)
         waitForSchemaToIncrement(generation)
      }
   }

   companion object {

      @Container
      private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1")

      @BeforeClass
      @JvmStatic
      fun before() {
         postgreSQLContainer.start()
      }

      @JvmStatic
      @DynamicPropertySource
      fun registerDynamicProperties(registry: DynamicPropertyRegistry) {

         postgreSQLContainer.waitingFor(Wait.forListeningPort())

         registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl)
         registry.add("spring.datasource.username", postgreSQLContainer::getUsername)
         registry.add("spring.datasource.password", postgreSQLContainer::getPassword)

         registry.add("spring.flyway.url", postgreSQLContainer::getJdbcUrl)
         registry.add("spring.flyway.username", postgreSQLContainer::getUsername)
         registry.add("spring.flyway.password", postgreSQLContainer::getPassword)
         registry.add("spring.flyway.validate-on-migrate", {false})

      }

   }

   @TestConfiguration
   class SpringConfig {

      @Bean
      @Primary
      fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
         val jdbcTemplate = JdbcTemplate(dataSource)
         jdbcTemplate.execute(TableMetadata.DROP_TABLE)
         return jdbcTemplate
      }

   }

   val caskRequest = """
Date,Symbol,Open,High,Low,Close
2020-03-19,BTCUSD,6300,6330,6186.08,6235.2
2020-03-19,NULL,6300,6330,6186.08,6235.2
2020-03-19,BTCUSD,6300,6330,6186.08,6235.2
2020-03-19,ETHUSD,6300,6330,6186.08,6235.2""".trimIndent()

   val caskRequestWithPrologue = """
this is a bunch
of
non csv </dos>asdfasdflj£!" , , , , , , ,
rubbish
$caskRequest
      """.trimIndent()

   val caskRequestWithPipeAsDelimiter = """
Date|Symbol|Open|High|Low|Close
2020-03-19|BTCUSD|6300|6330|6186.08|6235.2
2020-03-19|NULL|6300|6330|6186.08|6235.2
2020-03-19|BTCUSD|6300|6330|6186.08|6235.2
2020-03-19|ETHUSD|6300|6330|6186.08|6235.2""".trimIndent()

   @Test
   fun canIngestContentViaWebsocketConnection() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = CustomReactorNettyWebsocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")

      val wsConnection = client.execute(uri)
      { session ->
         session.send(Mono.just(session.textMessage(caskRequest)))
            .thenMany(
               session.receive()
                  .log()
                  .map(WebSocketMessage::getPayloadAsText)
                  .subscribeWith(output)
            )
            .then()
      }.subscribe()

      StepVerifier
         .create(output.take(1).timeout(Duration.ofSeconds(10000)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .verifyComplete()
         .run { wsConnection.dispose() }
   }

   @Test
   fun `after removing a cask using CaskService, its types and services are removed from the schema`() {
      // mock schema
      log().info("Starting test after removing a cask using CaskService, its types and services are removed from the schema")
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      var lastObservedGeneration = schemaStoreClient.generation
      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      val response = client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()

      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")

      // Ensure casks and types have been created
      lastObservedGeneration = waitForSchemaToIncrement(lastObservedGeneration)
      val schemaAfterIngestion = schemaProvider.schema()
      val caskTypeNames = schemaAfterIngestion.types.filter {
         it.name.namespace.startsWith(DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE)
      }.map { it.fullyQualifiedName }
      caskTypeNames.should.contain.elements("vyne.cask.OrderWindowSummaryCsv")
      val serviceNames = schemaAfterIngestion.services
         .filter { it.qualifiedName.startsWith(DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE) }
         .map { it.qualifiedName }
      serviceNames.should.contain.elements("vyne.cask.OrderWindowSummaryCsvCaskService")
      caskService.deleteCasks(configRepository.findAll())

      // Wait until schemas have been modified and republished after the deletion
      // (Happens async)
      lastObservedGeneration = waitForSchemaToIncrement(lastObservedGeneration)
      val schemaAfterDeletion = schemaProvider.schema()
      val caskTypeNamesAfterDeletion = schemaAfterDeletion.types.filter {
         it.name.namespace.startsWith(DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE)
      }.map { it.fullyQualifiedName }


      caskTypeNamesAfterDeletion.should.not.contain.elements("vyne.cask.OrderWindowSummaryCsv")

      val serviceNamesAfterDeletion = schemaAfterDeletion.services
         .filter { it.qualifiedName.startsWith(DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE) }
         .map { it.qualifiedName }
      serviceNamesAfterDeletion.should.not.contain.elements("vyne.cask.OrderWindowSummaryCsvCaskService")
   }

   private fun waitForSchemaToIncrement(lastObservedGeneration: Int):Int {
      await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until<Boolean> {
         schemaStoreClient.generation > lastObservedGeneration
      }
      return schemaStoreClient.generation
   }

   @Test
   fun `can ingest content via websocket with ignored prologue`() {
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = CustomReactorNettyWebsocketClient()
      val uri =
         URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=,&ignoreContentBefore=Date,Symbol,Open")


      val wsConnection = client.execute(uri)
      { session ->
         session.send(Mono.just(session.textMessage(caskRequest)))
            .thenMany(
               session.receive()
                  .log()
                  .map(WebSocketMessage::getPayloadAsText)
                  .subscribeWith(output)
            )
            .then()
      }.subscribe()

      StepVerifier
         .create(output.take(1).timeout(Duration.ofSeconds(10000)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .verifyComplete()
         .run { wsConnection.dispose() }
   }

   @Test
   fun `Can Ingest via websocket for schema with default and concat definitions`() {
      // mock schema
      schemaPublisher.submitSchema("default-concat-schemas", "1.0.0", schemaWithConcatAndDefaultSource)

      val csvData = """
FIRST_COLUMN,SECOND_COLUMN,THIRD_COLUMN
1,2,3
4,5,6
7,8,9
10,11,2""".trimIndent()

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = CustomReactorNettyWebsocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/ModelWithDefaultsConcat?debug=true&delimiter=,")


      val wsConnection = client.execute(uri)
      { session ->
         session.send(Mono.just(session.textMessage(csvData)))
            .thenMany(
               session.receive()
                  .log()
                  .map(WebSocketMessage::getPayloadAsText)
                  .subscribeWith(output)
            )
            .then()
      }.subscribe()

      StepVerifier
         .create(output.take(1).timeout(Duration.ofSeconds(10)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .verifyComplete()
         .run { wsConnection.dispose() }
   }

   @Test
   fun canIngestLargeContentViaWebsocketConnection() {
      var caskRequest = """Date,Symbol,Open,High,Low,Close"""
      for (i in 1..10000) {
         caskRequest += "\n2020-03-19,BTCUSD,6300,6330,6186.08,6235.2"
      }
      caskRequest.length.should.be.above(20000) // Default websocket buffer size is 8096

      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = CustomReactorNettyWebsocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")

      val wsConnection = client.execute(uri)
      { session ->
         session.send(Mono.just(session.textMessage(caskRequest)))
            .thenMany(
               session.receive()
                  .log()
                  .map(WebSocketMessage::getPayloadAsText)
                  .subscribeWith(output)
            )
            .then()
      }.subscribe()

      StepVerifier
         .create(output.take(1).timeout(Duration.ofSeconds(10)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 10000 records"}""")
         .verifyComplete()
         .run { wsConnection.dispose() }
   }

   @Test
   fun canIngestContentViaRestEndpoint() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      val response = client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()

      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
   }

   @Test
   fun canIngestContentViaRestEndpointWithPipeDelimiter() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      val response = client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?delimiter=|&firstRecordAsHeader=true&containsTrailingDelimiters=false")
         .bodyValue(caskRequestWithPipeAsDelimiter)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()

      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
   }

   @Test
   fun canIngestContentWithProloguseViaRestEndpoint() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      val response = client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&delimiter=,&ignoreContentBefore=Date,Symbol")
         .bodyValue(caskRequestWithPrologue)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()

      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
   }


   @Test
   fun canIngestLargeContentViaRestEndpoint() {
      var caskRequest = """Date,Symbol,Open,High,Low,Close"""
      for (i in 1..10000) {
         caskRequest += "\n2020-03-19,BTCUSD,6300,6330,6186.08,6235.2"
      }
      caskRequest.length.should.be.above(20000)

      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      val response = client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()

      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 10000 records"}""")
   }

   @Test
   fun canQueryForCaskData() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      var client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()
         .should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")

      client
         .post()
         .uri("/api/cask/OrderWindowSummaryCsv/symbol/ETHBTC")
         .bodyValue(caskRequest)
         .accept(MediaType.APPLICATION_JSON)
         .retrieve()
         .bodyToFlux(String::class.java)
         .blockLast()
         .should.be.equal("[]")

      val result = client
         .post()
         .uri("/api/cask/OrderWindowSummaryCsv/symbol/ETHUSD")
         .bodyValue(caskRequest)
         .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
         .retrieve()
         .bodyToFlux(OrderWindowSummaryDto::class.java)
         .collectList().block()


      result.should.not.be.empty

      // assert date coming back from Postgresql is equal to what was sent to cask for ingestion
     //result[0].orderDate
     //    .toInstant().atZone(ZoneId.of("UTC")).toLocalDate()
     //    .should.be.equal(LocalDate.parse("2020-03-19"))
   }

   @Test
   @Ignore
   fun `Can ingest when schema is upgraded`() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val outputAfterSchemaChanged: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = ReactorNettyWebSocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")

      val wsConnection = client.execute(uri)
      { session ->
         session.send(MessagePublisher(session, caskRequest, schemaPublisher, caskServiceBootstrap))
            .thenMany(
               session.receive()
                  .log()
                  .map(WebSocketMessage::getPayloadAsText)
                  .subscribeWith(output)
            )
            .then()

      }.subscribe()



      StepVerifier
         .create(output.take(2).timeout(Duration.ofSeconds(10000)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         //.expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .verifyComplete()
         .run { wsConnection.dispose() }
   }

   @Test
   fun `Can Query Cask Data with a field backed by database timestamp column`() {
      val lastObservedGeneration = schemaStoreClient.generation
      // mock schema
      schemaPublisher.submitSchema(
         "test-schemas",
         "1.0.0",
         CoinbaseJsonOrderSchema.sourceV1.plus(
            """
            model RfqDateModel {
                changeDateTime : Instant? (@format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'") by column(1)
            }
         """.trimIndent()
         )
      )

      waitForSchemaToIncrement(lastObservedGeneration)

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      val caskRequest = """
changeTime
2020-12-11T08:08:13.1792973Z
""".trimIndent()

      client
         .post()
         .uri("/api/ingest/csv/RfqDateModel?debug=true&delimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()
         .should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")


      val result = client
         .post()
         .uri("/api/cask/findAll/RfqDateModel")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToFlux(String::class.java)
         .blockFirst()

      result.should.not.be.empty
      val queryResult = result.drop(1).dropLast(1) // drop [ and ]
      queryResult.should.startWith("""{"changeDateTime":1607674093.179297000,"caskmessageid":""")
   }


   @Test
   fun canVyneQLQueryForListResponse() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val client = WebClient
         .builder()
         .exchangeStrategies(ExchangeStrategies
            .builder()
            .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
            .build())
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()
         .should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")


      val body = webTestClient
         .mutate()
         .exchangeStrategies(ExchangeStrategies
            .builder()
            .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(Int.MAX_VALUE) }
            .build())
         .build()
         .post()
         .uri(VyneQlQueryService.REST_ENDPOINT)
         .contentType(MediaType.APPLICATION_JSON)
         .accept(MediaType.APPLICATION_JSON)
         .bodyValue("""findAll { OrderWindowSummaryCsv[] }""")
         .exchange()
         .expectStatus().isOk()
         .expectHeader().contentType(MediaType.APPLICATION_JSON)
         .expectBody()
         .jsonPath("$.length()").isEqualTo(4)

   }


   @Test
   fun canVyneQLQueryForStreamedResponse() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()
         .should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")


      val result = webTestClient
         .post()
         .uri(VyneQlQueryService.REST_ENDPOINT)
         .accept(MediaType.valueOf(MediaType.TEXT_EVENT_STREAM_VALUE))
         .bodyValue("""findAll { OrderWindowSummaryCsv[] }""")
         .exchange()
         .expectStatus().isOk()
         .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
         .returnResult<Any>()

      StepVerifier.create(result.getResponseBody())
         .expectSubscription()
         .expectNextCount(4)
         .thenCancel()
         .verify()


   }


   class MessagePublisher(
      val session: WebSocketSession,
      val messageContent: String,
      val schemaPublisher: SchemaPublisher,
      val caskServiceBootstrap: CaskServiceBootstrap
   ) : Publisher<WebSocketMessage> {
      override fun subscribe(subscriber: Subscriber<in WebSocketMessage>) {
         subscriber.onSubscribe(object : Subscription {
            override fun request(p0: Long) {
               subscriber.onNext(session.textMessage(messageContent))

               schemaPublisher.submitSchema("test-schemas", "1.0.1", CoinbaseJsonOrderSchema.CsvWithDefault).map {
                  val schemaStoreClient = schemaPublisher as SchemaStoreClient
                  caskServiceBootstrap.regenerateCasksOnSchemaChange(
                     SchemaSetChangedEvent(
                        null,
                        schemaStoreClient.schemaSet()
                     )
                  )
                  //   caskServiceBootstrap.onIngesterInitialised(IngestionInitialisedEvent(this,
                  //        VersionedType(it.sources, it.type("OrderWindowSummaryCsv"), it.taxiType(QualifiedName("OrderWindowSummaryCsv")))))
               }

               Thread.sleep(2000)
               subscriber.onNext(session.textMessage(messageContent))
               subscriber.onComplete()
            }

            override fun cancel() {
               TODO("Not yet implemented")
            }

         })


      }

   }

   data class OrderWindowSummaryDto(
      val orderDate: Date,
      val symbol: String,
      val open: Double,
      val close: Double
   )

   data class RfqDateModelDto(val changeDateTime: Instant)

   class CustomReactorNettyWebsocketClient : ReactorNettyWebSocketClient() {

      override fun execute(url: URI, requestHeaders: HttpHeaders?, handler: WebSocketHandler): Mono<Void?>? {
         return httpClient
            .headers { nettyHeaders: io.netty.handler.codec.http.HttpHeaders? ->
               setNettyHeaders(
                  requestHeaders,
                  nettyHeaders
               )
            }
            .websocket()
            .uri(url.toString())
            .handle<Void>(BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> { inbound: WebsocketInbound?, outbound: WebsocketOutbound ->
               val responseHeaders = toHttpHeaders(inbound)
               val protocol = responseHeaders?.getFirst("Sec-WebSocket-Protocol")
               val info = HandshakeInfo(url, responseHeaders, Mono.empty(), protocol)
               val factory = NettyDataBufferFactory(outbound.alloc())
               val session: WebSocketSession = ReactorNettyWebSocketSession(
                  inbound, outbound, info, factory, maxFramePayloadLength
               )

               handler.handle(session).checkpoint("$url [ReactorNettyWebSocketClient]")
            })
            .next()
      }

      private fun setNettyHeaders(httpHeaders: HttpHeaders?, nettyHeaders: io.netty.handler.codec.http.HttpHeaders?) {
         httpHeaders?.forEach { s: String?, iterable: List<String?>? ->
            nettyHeaders?.set(s, iterable)
         }
      }

      private fun toHttpHeaders(inbound: WebsocketInbound?): HttpHeaders? {
         val headers = HttpHeaders()
         val nettyHeaders = inbound?.headers()
         nettyHeaders?.forEach(Consumer { entry: Map.Entry<String, String?> ->
            val name = entry.key
            headers[name] = nettyHeaders.getAll(name)
         })
         return headers
      }

   }
}


