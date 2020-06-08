package io.vyne.cask

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.spring.SchemaPublicationMethod
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.utils.log
import org.junit.AfterClass
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import javax.annotation.PreDestroy

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false"
   ])
@VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISABLED)
@ActiveProfiles("test")
class CaskAppIntegrationTest {
   @LocalServerPort
   val randomServerPort = 0

   @Autowired
   lateinit var schemaStoreClient: SchemaStoreClient


   companion object {
      lateinit var pg: EmbeddedPostgres

      @BeforeClass
      @JvmStatic
      fun setupDb() {
         // port used in the config by the Flyway, hence hardcoded
         pg =  EmbeddedPostgres.builder().setPort(6662).start()
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

   @Test
   fun canIngestContentViaWebsocketConnection() {
      // mock schema
      schemaStoreClient.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)

      val output: EmitterProcessor<String> = EmitterProcessor.create()
      val client: WebSocketClient = ReactorNettyWebSocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&csvDelimiter=,")
      val caskRequest = """
Date,Symbol,Open,High,Low,Close
2020-03-19,BTCUSD,6300,6330,6186.08,6235.2
2020-03-19,NULL,6300,6330,6186.08,6235.2
2020-03-19,BTCUSD,6300,6330,6186.08,6235.2
2020-03-19,BTCUSD,6300,6330,6186.08,6235.2""".trimIndent()

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
}
