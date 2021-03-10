package io.vyne.cask

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.winterbe.expekt.should
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.ingest.TestSchema.schemaWithConcatAndDefaultSource
import io.vyne.cask.query.generators.OperationGeneratorConfig
import io.vyne.cask.query.vyneql.VyneQlQueryService
import io.vyne.cask.services.CaskServiceBootstrap
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.annotation.PreDestroy
import javax.sql.DataSource

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.schema.publicationMethod=LOCAL"
   ])
@ActiveProfiles("test")
@EnableConfigurationProperties(OperationGeneratorConfig::class)
class CaskAppIntegrationTest {
   @LocalServerPort
   val randomServerPort = 0

   @Autowired
   lateinit var schemaPublisher: SchemaPublisher

   @Autowired
   lateinit var caskServiceBootstrap: CaskServiceBootstrap

   @Autowired
   lateinit var webTestClient: WebTestClient

   companion object {
      lateinit var pg: EmbeddedPostgres
      @BeforeClass
      @JvmStatic
      fun setupDb() {
         // port used in the config by the Flyway, hence hardcoded
         pg =  EmbeddedPostgres.builder().setPort(6662).start()
         pg.postgresDatabase.connection
      }

      @AfterClass
      @JvmStatic
      fun cleanupdb() {
         pg.close()
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

      @PreDestroy
      fun destroy() {
         log().info("Closing embedded Postgres...")
         // As long as we don't have dirty context, the PostConstruct should be fine. Close again AfterClass just in case. Doesn't hurt
         pg.close()
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
      val client: WebSocketClient = ReactorNettyWebSocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")

      val wsConnection = client.execute(uri)
      { session ->
         session.send(Mono.just(session.textMessage(caskRequest)))
            .thenMany(session.receive()
               .log()
               .map(WebSocketMessage::getPayloadAsText)
               .subscribeWith(output))
            .then()
      }.subscribe()

      StepVerifier
         .create(output.take(1).timeout(Duration.ofSeconds(10000)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .verifyComplete()
         .run { wsConnection.dispose() }
   }

   @Test
   fun `can ingest content via websocket with ignored prologue`() {


      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = ReactorNettyWebSocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=,&ignoreContentBefore=Date,Symbol,Open")

      val wsConnection = client.execute(uri)
      { session ->
         session.send(Mono.just(session.textMessage(caskRequest)))
            .thenMany(session.receive()
               .log()
               .map(WebSocketMessage::getPayloadAsText)
               .subscribeWith(output))
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
      val client: WebSocketClient = ReactorNettyWebSocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/ModelWithDefaultsConcat?debug=true&delimiter=,")

      val wsConnection = client.execute(uri)
      { session ->
         session.send(Mono.just(session.textMessage(csvData)))
            .thenMany(session.receive()
               .log()
               .map(WebSocketMessage::getPayloadAsText)
               .subscribeWith(output))
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
      for(i in 1..10000){
         caskRequest += "\n2020-03-19,BTCUSD,6300,6330,6186.08,6235.2"
      }
      caskRequest.length.should.be.above(20000) // Default websocket buffer size is 8096

      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = ReactorNettyWebSocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")

      val wsConnection = client.execute(uri)
      { session ->
         session.send(Mono.just(session.textMessage(caskRequest)))
            .thenMany(session.receive()
               .log()
               .map(WebSocketMessage::getPayloadAsText)
               .subscribeWith(output))
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
      for(i in 1..10000){
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

      client
         .post()
         .uri("/api/cask/OrderWindowSummaryCsv/symbol/ETHBTC")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()
         .should.be.equal("[]")

      val result = client
         .post()
         .uri("/api/cask/OrderWindowSummaryCsv/symbol/ETHUSD")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToFlux(OrderWindowSummaryDto::class.java)
         .collectList()
         .block()

      result.should.not.be.empty

      // assert date coming back from Postgresql is equal to what was sent to cask for ingestion
      result[0].orderDate
         .toInstant().atZone(ZoneId.of("UTC")).toLocalDate()
         .should.be.equal(LocalDate.parse("2020-03-19"))
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
            .thenMany(session.receive()
               .log()
               .map(WebSocketMessage::getPayloadAsText)
               .subscribeWith(output))
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
      // mock schema
      schemaPublisher.submitSchema(
         "test-schemas",
         "1.0.0",
         CoinbaseJsonOrderSchema.sourceV1.plus("""
            model RfqDateModel {
                changeDateTime : Instant? (@format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'") by column(1)
            }
         """.trimIndent()))

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

      webTestClient
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


   class MessagePublisher(val session: WebSocketSession, val messageContent: String, val schemaPublisher: SchemaPublisher,  val caskServiceBootstrap: CaskServiceBootstrap): Publisher<WebSocketMessage> {
      override fun subscribe(subscriber: Subscriber<in WebSocketMessage>) {
         subscriber.onSubscribe( object: Subscription {
            override fun request(p0: Long) {
               subscriber.onNext(session.textMessage(messageContent))

               schemaPublisher.submitSchema("test-schemas", "1.0.1", CoinbaseJsonOrderSchema.CsvWithDefault).map {
                  val schemaStoreClient = schemaPublisher as SchemaStoreClient
                   caskServiceBootstrap.regenerateCasksOnSchemaChange(SchemaSetChangedEvent(null, schemaStoreClient.schemaSet()))
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
}
