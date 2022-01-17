package io.vyne.cask

import arrow.core.Either
import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.ingest.TestSchema.schemaWithConcatAndDefaultSource
import io.vyne.cask.observers.ObservedChange
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.query.generators.OperationGeneratorConfig
import io.vyne.cask.query.vyneql.VyneQlQueryService
import io.vyne.cask.services.CaskServiceBootstrap
import io.vyne.cask.services.DefaultCaskTypeProvider
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemaConsumerApi.SchemaStore
import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
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
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.netty.http.websocket.WebsocketInbound
import reactor.netty.http.websocket.WebsocketOutbound
import reactor.test.StepVerifier
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.HashMap
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
@EmbeddedKafka(
   partitions = 1,
   topics = ["\${vyne.connections.kafka[0].topic}"],
   brokerProperties = [
      "listeners=PLAINTEXT://\${vyne.connections.kafka[0].bootstrap-servers}",
      "auto.create.topics.enable=\${kafka.broker.topics-enable:true}"
   ]
)

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
   lateinit var schemaStore: SchemaStore

   @Autowired
   lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

   lateinit var kafkaMessageListener: KafkaTestMessageListener

   lateinit var kafkaMessageListenerContainer: KafkaMessageListenerContainer<String, ObservedChange>

   @Value("\${vyne.connections.kafka[0].topic}")
   lateinit var writeToTopic: String

   @Before
   fun beforeEach() {
      val configs = HashMap(KafkaTestUtils.consumerProps("consumer", "false", embeddedKafkaBroker))
      // we're using JsonDeserializer see below, so we need to specify the type of message values.
      configs[org.springframework.kafka.support.serializer.JsonDeserializer.VALUE_DEFAULT_TYPE] = ObservedChange::class.java
      val consumerFactory = DefaultKafkaConsumerFactory(
         configs,
         StringDeserializer(),
         org.springframework.kafka.support.serializer.JsonDeserializer(ObservedChange::class.java))
      // see application-test.yml for the topic name setting.
      val containerProperties = ContainerProperties(writeToTopic)
      kafkaMessageListenerContainer = KafkaMessageListenerContainer(consumerFactory, containerProperties)
      kafkaMessageListener = KafkaTestMessageListener()
      kafkaMessageListenerContainer.setupMessageListener(kafkaMessageListener)
      kafkaMessageListenerContainer.start()
      ContainerTestUtils.waitForAssignment(kafkaMessageListenerContainer, embeddedKafkaBroker.partitionsPerTopic)
   }

   @After
   fun tearDown() {
      val caskConfigs = configRepository.findAll()
      if (caskConfigs.isNotEmpty()) {
         val generation = schemaStore.generation
         caskService.deleteCasks(caskConfigs)
         waitForSchemaToIncrement(generation)
      }
      kafkaMessageListenerContainer.stop()
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

      var lastObservedGeneration = schemaStore.generation
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
         schemaStore.generation > lastObservedGeneration
      }
      return schemaStore.generation
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

       //assert date coming back from Postgresql is equal to what was sent to cask for ingestion
       result[0].orderDate
         .toInstant().atZone(ZoneId.of("UTC")).toLocalDate()
         .should.be.equal(LocalDate.parse("2020-03-19"))
   }

   @Test
   @Ignore("This test is wrong and hence intemittently fails, fixed in history server branch so commenting out here.")
   fun `Can ingest when schema is upgraded`() {
      var lastObservedGeneration = schemaStore.generation
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.sourceV1)
      waitForSchemaToIncrement(lastObservedGeneration)
      val textOutput = Sinks.many().multicast().onBackpressureBuffer<String>()
      val textOutputFlux = textOutput.asFlux()
      val output = Sinks.many().replay().limit<String>(5)
      val client: WebSocketClient = CustomReactorNettyWebsocketClient()
      val uri = URI.create("ws://localhost:${randomServerPort}/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=,")
      var schemaUpgrader = SchemaUpgrader(schemaPublisher, caskServiceBootstrap, schemaProvider)
      val receivedIngestionResponses = mutableListOf<String>()
      lastObservedGeneration = schemaStore.generation
      val wsConnection = client.execute(uri)
      { session ->
         session.send(textOutputFlux.map(session::textMessage))
            .and(
               session.receive()
                  .map(WebSocketMessage::getPayloadAsText)
                  .doOnEach { response ->
                     if (response.isOnNext) {
                        receivedIngestionResponses.add(response.get()!!)
                        output.tryEmitNext(response.get()!!)
                     }
                  }
            ).then()
      }.subscribe()


      textOutput.tryEmitNext(caskRequest)
      waitForSchemaToIncrement(lastObservedGeneration)

      // First message
      StepVerifier
         .create(output.asFlux().take(1).timeout(Duration.ofSeconds(20)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .verifyComplete()


      //second message
      val singleMessage = """
         Date,Symbol,Open,High,Low,Close
         2020-03-19,BTCUSD,6300,6330,6186.08,6235.2""".trimIndent()

      textOutput.tryEmitNext(singleMessage)
      StepVerifier
         .create(output.asFlux().take(2).timeout(Duration.ofSeconds(20)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()

      val targetCaskConfigs = configRepository.findAllByQualifiedTypeName("OrderWindowSummaryCsv")
      targetCaskConfigs.size.should.equal(1)
      val currentTableName = targetCaskConfigs.first().tableName
      // Now Upgrade the schema whilst the web socket session is active
      StepVerifier.create(schemaUpgrader.performSchemaUpgrade("1.0.2"))
         .expectNextMatches { nextVersion -> nextVersion != null }
         .verifyComplete()
      // Wait for the migration to finish
      await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until<Boolean> {
         val updatedConfigs  = configRepository.findAllByQualifiedTypeName("OrderWindowSummaryCsv")
         updatedConfigs.size == 1 && updatedConfigs.first().tableName != currentTableName
      }
      // We now have a new Cask Table name for OrderWindowSummaryCsv inject more items into it from the existing
      // Websocket session, this should trigger 'table not found' PSQLExceptions but we should be able to recover from that.
      // and ingest items into the new Table.
      textOutput.tryEmitNext("""
         Date,Symbol,Open,High,Low,Close
         2020-03-19,BTCUSD,6300,6330,6186.08,6235.2""".trimIndent())

      StepVerifier
         .create(output.asFlux().take(4).timeout(Duration.ofSeconds(20)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .expectNext("""{"result":"REJECTED","message":"org.postgresql.util.PSQLException: ERROR: relation \"rwindowsummarycsv_d3c664_81a347\" does not exist"}""")
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()

   }

   @Test
   fun `Can Query Cask Data with a field backed by database timestamp column`() {
      val lastObservedGeneration = schemaStore.generation
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

   @Test
   fun `Can ingest observable type with primary keys and publish changes to kafka`() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.observableCoinbaseWithPk)
      val postData = """
Date,Symbol,Open,High,Low,Close
19/03/2019,BTCUSD,6300,6330,6186.08,6235.2
19/03/2019,ETHUSD,6300,6330,6186.08,6235.2
20/03/2019,BTCUSD,6301,6331,6186.08,6235.2
20/03/2019,ETHUSD,6200,6230,6186.08,6235.2""".trimIndent()

     val response = postCsvData(postData, "OrderWindowSummaryCsv")
      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
      StepVerifier
         .create(kafkaMessageListener.flux.take(4).timeout(Duration.ofSeconds(10)))
         .expectNext(ObservedChange(
            ids = LinkedHashMap(mutableMapOf( """"symbol"""" to "\'BTCUSD\'")),
            current = mapOf("orderDate" to "19/03/2019", "symbol" to "BTCUSD", "open" to 6300, "close" to 6330),
            old = mapOf("close" to null, "open" to null, "orderDate" to null, "symbol" to null)
         ))
         .expectNext(ObservedChange(
            ids = LinkedHashMap(mutableMapOf( """"symbol"""" to "\'ETHUSD\'")),
            current = mapOf("orderDate" to "19/03/2019", "symbol" to "ETHUSD", "open" to 6300, "close" to 6330),
            old = mapOf("close" to null, "open" to null, "orderDate" to null, "symbol" to null)
         ))
         .expectNext(ObservedChange(
            ids = LinkedHashMap(mutableMapOf( """"symbol"""" to "\'BTCUSD\'")),
            current = mapOf("orderDate" to "20/03/2019", "symbol" to "BTCUSD", "open" to 6301, "close" to 6331),
            old = mapOf("orderDate" to "19/03/2019", "symbol" to "BTCUSD", "open" to 6300, "close" to 6330)
         ))
         .expectNext(ObservedChange(
            ids = LinkedHashMap(mutableMapOf( """"symbol"""" to "\'ETHUSD\'")),
            current = mapOf("orderDate" to "20/03/2019", "symbol" to "ETHUSD", "open" to 6200, "close" to 6230),
            old = mapOf("orderDate" to "19/03/2019", "symbol" to "ETHUSD", "open" to 6300, "close" to 6330)
         ))
         .verifyComplete()
   }

   @Test
   fun `Can ingest observable type  and publish changes to kafka`() {
      // mock schema
      schemaPublisher.submitSchema("test-schemas", "1.0.0", CoinbaseJsonOrderSchema.observableCoinbase)
      val postData = """
Date,Symbol,Open,High,Low,Close
19/03/2019,BTCUSD,6300,6330,6186.08,6235.2
19/03/2019,ETHUSD,6300,6330,6186.08,6235.2
20/03/2019,BTCUSD,6301,6331,6186.08,6235.2
20/03/2019,ETHUSD,6200,6230,6186.08,6235.2""".trimIndent()

      val response = postCsvData(postData, "OrderWindowSummaryCsv")
      response.should.be.equal("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
      StepVerifier
         .create(kafkaMessageListener.flux.take(4).timeout(Duration.ofSeconds(10)))
         .thenConsumeWhile { observedChange ->
            observedChange.ids.containsKey("cask_raw_id").should.be.`true`
            observedChange.old.should.be.`null`
            observedChange.current.size == 4
         }
         .verifyComplete()
   }

   private fun postCsvData(postData: String, typeName: String): String? {
      val client = WebClient
         .builder()
         .baseUrl("http://localhost:${randomServerPort}")
         .build()

      return client
         .post()
         .uri("/api/ingest/csv/$typeName?debug=true&delimiter=,")
         .bodyValue(postData)
         .retrieve()
         .bodyToMono(String::class.java)
         .block()
   }


   class SchemaUpgrader(
      val schemaPublisher: SchemaPublisher,
      val caskServiceBootstrap: CaskServiceBootstrap,
      val schemaProvider: SchemaProvider
   ) {
      fun performSchemaUpgrade(schemaVersion: String): Mono<String> {
         val currentSemanticVersion = schemaProvider.sources().first().semver
         val monoOrError = schemaPublisher.submitSchema("test-schemas", schemaVersion, CoinbaseJsonOrderSchema.CsvWithDefault).map {
            val schemaStoreClient = schemaPublisher as SchemaStore
            caskServiceBootstrap.regenerateCasksOnSchemaChange(
               SchemaSetChangedEvent(
                  null,
                  schemaStoreClient.schemaSet()
               )
            )

            Mono.fromCallable {
               if (schemaProvider.sources().first().semver > currentSemanticVersion) {
                  schemaProvider.sources().first().version
               } else {
                  throw IllegalStateException("version must be greater than $currentSemanticVersion")
               }
            }.retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(1)))
         }

         return when(monoOrError) {
            is Either.Left -> Mono.error(IllegalStateException("version must be 1.0.1"))
            is Either.Right -> monoOrError.b
         }
      }
   }

   data class OrderWindowSummaryDto(
      val orderDate: Date,
      val symbol: String,
      val open: Double,
      val close: Double
   )

   class KafkaTestMessageListener(): MessageListener<String, ObservedChange> {
      private val replaySink = Sinks.many().replay().all<ObservedChange>()
      val flux = replaySink.asFlux()
      override fun onMessage(record: ConsumerRecord<String, ObservedChange>?) {
         replaySink.tryEmitNext(record?.value())
      }
   }

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


