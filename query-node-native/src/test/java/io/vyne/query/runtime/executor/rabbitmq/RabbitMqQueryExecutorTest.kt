package io.vyne.query.runtime.executor.rabbitmq

import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.awaitility.Awaitility
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.rabbitmq.client.Address
import com.rabbitmq.client.ConnectionFactory
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.vyne.auth.tokens.AuthConfig
import io.vyne.connectors.config.ConnectorsConfig
import io.vyne.http.ServicesConfig
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponseMessage
import io.vyne.query.runtime.QueryMessage
import io.vyne.query.runtime.QueryMessageCborWrapper
import io.vyne.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin
import io.vyne.query.runtime.executor.QueryExecutor
import io.vyne.query.runtime.executor.StandaloneVyneFactory
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import io.vyne.utils.Ids
import lang.taxi.utils.log
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.springframework.http.MediaType
import org.testcontainers.containers.RabbitMQContainer
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import reactor.rabbitmq.*
import reactor.test.StepVerifier
import java.time.Duration

class RabbitMqQueryExecutorTest {
   @Rule
   @JvmField
   var rabbitContainer: RabbitMQContainer = RabbitMQContainer("rabbitmq:3.11.13-management")
      .withExposedPorts(5672, 15672)
      .withVhost("/")
      .withUser("admin", "admin")
      .withPermission("/", "admin", ".*", ".*", ".*")

   lateinit var rabbitSender: Sender
   lateinit var rabbitReceiver: Receiver
   val objectMapper = Jackson.newObjectMapperWithDefaults()
   lateinit var queryExecutor: RabbitMqQueryExecutor
   lateinit var vyneFactory: StandaloneVyneFactory

   @Before
   fun setup() {
      StepVerifier.setDefaultTimeout(Duration.ofSeconds(10))
      log().info("Admin user/pass for rabbitMQ: ${rabbitContainer.adminUsername} / ${rabbitContainer.adminPassword} http port: ${rabbitContainer.httpPort}")
      val connectionFactory = ConnectionFactory()
      connectionFactory.useNio()
      val address = Address(rabbitContainer.host, rabbitContainer.amqpPort)
      rabbitSender = RabbitAdmin.rabbitSender(connectionFactory, address)
      rabbitReceiver = RabbitAdmin.rabbitReceiver(connectionFactory, address)
      vyneFactory = mock { }
   }

   private fun initQueryExecutor(parallelism: Int): RabbitMqQueryExecutor {
      queryExecutor = createQueryExecutor(parallelism)

      queryExecutor.setupRabbit()
         .block()
      return queryExecutor
   }

   @Test
   fun executesQueryAndWritesResponsesToRabbit() {
      initQueryExecutor(parallelism = 1)
      val (vyne, stub) = testVyne(TaxiSchema.empty())
      whenever(vyneFactory.buildVyne(any())).thenReturn(vyne to mock { })
      val queryId = Ids.id("query-")

      val query = """given { f:String = 'Hello, world' } find { response : String }"""

      queryExecutor.consumeAndExecuteQueries()
         .test()
         .expectSubscription()
         .then {
            sendQuery(query, queryId)
         }
         .expectNextMatches { (messageKind, outboundMessageResult) ->
            verifyReceivedMessageMatches(
               outboundMessageResult,
               queryId,
               QueryResponseMessage.QueryResponseMessageKind.STREAM_MESSAGE,
               mapOf("response" to "Hello, world")
            )
         }
         .expectNextMatches { (messageKind, outboundMessageResult) ->
            verifyReceivedMessageMatches(
               outboundMessageResult,
               queryId,
               QueryResponseMessage.QueryResponseMessageKind.END_OF_STREAM,
            )
         }
         // We cancel here.  The consuming flux never completes,
         // as it stays open to receive new queries.
         .thenCancel()
         .verify()
   }

   @Test
   @Ignore("Can't work out how to test this consistently.")
   fun `when parallel execution is disabled then queries execute sequentially`() {
      initQueryExecutor(parallelism = 5)
      val (vyne, stub) = testVyne(TaxiSchema.empty())
      whenever(vyneFactory.buildVyne(any())).thenReturn(vyne to mock())

      val query = """given { f:String = 'Hello, world' } find { response : String }"""
      val collectedMessages = mutableListOf<OutboundMessageResult<OutboundMessage>>()

      // Send three queries in parallel
      sendQuery(query, "query-1")
      sendQuery(query, "query-2")
      sendQuery(query, "query-3")


      queryExecutor.consumeAndExecuteQueries()
         .take(6)
         .subscribe { (messageKind, message) ->
            objectMapper.readValue<QueryResponseMessage>(message.outboundMessage.body)
            collectedMessages.add(message)
         }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.FIVE_SECONDS)
         .until<Boolean> { collectedMessages.size == 6 }

      // For each of the 3 queries we executed, we should have received - in order -
      // a stream result message, followed by an end of stream message.
      repeat(3) { i ->
         val queryId = "query-${i + 1}"
         val messagesForQuery = collectedMessages.filter { it.outboundMessage.routingKey == queryId }
            .map { objectMapper.readValue<QueryResponseMessage>(it.outboundMessage.body) }
         messagesForQuery.shouldHaveSize(2)
         messagesForQuery[0].messageKind.shouldBe(QueryResponseMessage.QueryResponseMessageKind.STREAM_MESSAGE)
         messagesForQuery[1].messageKind.shouldBe(QueryResponseMessage.QueryResponseMessageKind.END_OF_STREAM)
      }
   }

   private fun verifyReceivedMessageMatches(
      message: OutboundMessageResult<OutboundMessage>,
      queryId: String,
      messageType: QueryResponseMessage.QueryResponseMessageKind,
      expectedResult: Map<String, Any>? = null,
   ): Boolean {
      message.outboundMessage.exchange.shouldBe(RabbitAdmin.RESPONSES_EXCHANGE_NAME)
      message.outboundMessage.routingKey.shouldBe(queryId)
      val responseMessage =
         objectMapper.readValue<QueryResponseMessage>(message.outboundMessage.body)
      responseMessage.messageKind.shouldBe(messageType)
      responseMessage.payload.shouldBe(expectedResult)
      return true
   }

   private fun sendQuery(query: String, queryId: String) {
      val queryMessage = QueryMessageCborWrapper.from(
         QueryMessage(
            query,
            emptyList(),
            ConnectorsConfig.empty(),
            AuthConfig(),
            ServicesConfig.DEFAULT,
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            clientQueryId = queryId,
         )
      )
      val outboundMessage = OutboundMessage(
         RabbitAdmin.QUERY_EXCHANGE_NAME,
         RabbitAdmin.QUERIES_QUEUE_NAME,
         objectMapper.writeValueAsBytes(queryMessage)
      )
      val confirmation =
         rabbitSender.sendWithPublishConfirms(Mono.just(outboundMessage), SendOptions().trackReturned(true))
            .single()
            .block()!!
      confirmation.isReturned.shouldBeFalse()
      confirmation.isAck.shouldBeTrue()
      log().info("Query sent to Rabbit successfully")
   }

   private fun createQueryExecutor(parallelism: Int): RabbitMqQueryExecutor {
      val executor = QueryExecutor(vyneFactory, mock { })
      return RabbitMqQueryExecutor(
         rabbitSender,
         rabbitReceiver,
         executor,
         objectMapper,
         parallelism
      )
   }

}

