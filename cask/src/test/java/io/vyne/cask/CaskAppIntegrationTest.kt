package io.vyne.cask

import arrow.core.Either
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemas.Schema
import io.vyne.utils.log
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.WebSocketMessage
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
import java.util.*
import javax.annotation.PreDestroy

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.schema.publicationMethod=LOCAL"
   ])
@ActiveProfiles("test")
class CaskAppIntegrationTest {
   @LocalServerPort
   val randomServerPort = 0

   @Autowired
   lateinit var schemaPublisher: SchemaPublisher

   @Autowired
   lateinit var caskDao: CaskDAO

   lateinit var webClient: WebClient
   lateinit var schema: Schema

   @Before
   fun setup() {
      webClient = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      // mock schema
      val schemaResult = schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1) as Either.Right
      schema = schemaResult.b
   }

   companion object {
      lateinit var pg: EmbeddedPostgres

      @BeforeClass
      @JvmStatic
      fun setupDb() {
         // port used in the config by the Flyway, hence hardcoded
         pg = EmbeddedPostgres.builder().setPort(6662).start()
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
      fun jdbcTemplate(): JdbcTemplate {
         val dataSource = DataSourceBuilder.create()
            .url("jdbc:postgresql://localhost:${pg.port}/postgres")
            .username("postgres")
            .build()
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

   @Test
   fun canIngestContentViaWebsocketConnection() {

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = ReactorNettyWebSocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&csvDelimiter=,")

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
         .create(output.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .verifyComplete()
         .run { wsConnection.dispose() }
   }

   @Test
   fun canIngestLargeContentViaWebsocketConnection() {
      val caskRequest = insertRecords(10000)
      caskRequest.length.should.be.above(20000) // Default websocket buffer size is 8096

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = ReactorNettyWebSocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&csvDelimiter=,")

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

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      val response = client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&csvDelimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()

      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
   }

   @Test
   fun canIngestLargeContentViaRestEndpoint() {
      val caskRequest = insertRecords(10000)
      caskRequest.length.should.be.above(20000)

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      val response = client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&csvDelimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()

      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 10000 records"}""")
   }

   @Test
   fun canQueryForCaskData() {

      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      client
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&csvDelimiter=,")
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

   data class OrderWindowSummaryDto(
      val orderDate: Date,
      val symbol: String,
      val open: Double,
      val close: Double
   )


   @Test
   fun canEvictDataViaRestEndpoint() {
      val beginning = Instant.now()
      insertRecords(17)

      val middle = Instant.now()

      insertRecords(57)
      val end = Instant.now()

      val versionedTypeReference = VersionedTypeReference.parse("OrderWindowSummaryCsv")
      val versionedType = schema.versionedType(versionedTypeReference)
      caskDao.findAll(versionedType).size.should.be.equal(74)

      // No eviction
      evictQuery("rWindowSummaryCsv_d3c664_81a347", beginning.toString())
      caskDao.findAll(versionedType).size.should.be.equal(74)

      // 17 evictions
      evictQuery("rWindowSummaryCsv_d3c664_81a347", middle.toString())
      caskDao.findAll(versionedType).size.should.be.equal(57)

      // 57 evictions
      evictQuery("rWindowSummaryCsv_d3c664_81a347", end.toString())
      caskDao.findAll(versionedType).size.should.be.equal(0)

   }

   @Test
   fun canSetEvictionPeriod() {

      val versionedTypeReference = VersionedTypeReference.parse("OrderWindowSummaryCsv")
      val versionedType = schema.versionedType(versionedTypeReference)
      val caskConfig = caskDao.findAllCaskConfigs().first { it.qualifiedTypeName == versionedType.fullyQualifiedName }

      caskConfig.daysToRetain.should.equal(30) // Default is 30

      // Set 45 days eviction schedule
      setEvictionScheduleQuery(caskConfig.tableName, 45)

      val newCaskConfig = caskDao.findAllCaskConfigs().first { it.qualifiedTypeName == versionedType.fullyQualifiedName }
      newCaskConfig.daysToRetain.should.equal(45)

   }

   fun evictQuery(tableName: String, writtenBefore: String) {
      webClient
         .post()
         .uri("/api/casks/$tableName/evict")
         .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
         .bodyValue(""" { "writtenBefore": "$writtenBefore"}""")
         .retrieve()
         .bodyToMono(String::class.java)
         .block()
   }

   fun setEvictionScheduleQuery(tableName: String, daysToRetain: Int) {
      webClient
         .put()
         .uri("/api/casks/$tableName/evictSchedule")
         .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
         .bodyValue(""" { "daysToRetain": "$daysToRetain"}""")
         .retrieve()
         .bodyToMono(String::class.java)
         .block()
   }

   fun insertRecords(n: Int): String {
      var caskRequest = """Date,Symbol,Open,High,Low,Close"""
      for (i in 1..n) {
         caskRequest += "\n2020-03-19,BTCUSD,6300,6330,6186.08,6235.2"
      }

      val response = webClient
         .post()
         .uri("/api/ingest/csv/OrderWindowSummaryCsv?debug=true&csvDelimiter=,")
         .bodyValue(caskRequest)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()

      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested $n records"}""")
      return caskRequest
   }
}
