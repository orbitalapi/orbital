package com.orbitalhq.query.runtime.core.dispatcher.rabbitmq

import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.Address
import com.rabbitmq.client.ConnectionFactory
import io.kotest.matchers.shouldBe
import com.orbitalhq.auth.schemes.EmptyAuthSchemeRepository
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryFailedException
import com.orbitalhq.query.QueryResponseMessage
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.query.runtime.QueryMessageCborWrapper
import com.orbitalhq.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin.QUERIES_QUEUE_NAME
import com.orbitalhq.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin.RESPONSES_EXCHANGE_NAME
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.Ids
import lang.taxi.utils.log
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.testcontainers.containers.RabbitMQContainer
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.test.test
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.Receiver
import reactor.rabbitmq.Sender
import reactor.test.StepVerifier
import java.time.Duration

@Ignore("Currently failing.  No-one is using this at present, (using lambdas with http endpoints instead) so will park this")
class RabbitMqQueueDispatcherTest {
   @Rule
   @JvmField
   var rabbitContainer: RabbitMQContainer = RabbitMQContainer("rabbitmq:3.11.13-management")
      .withExposedPorts(5672, 15672)
      .withVhost("/")
      .withUser("admin", "admin")
      .withPermission("/", "admin", ".*", ".*", ".*");

   @Rule
   @JvmField
   val configRoot = TemporaryFolder()

   lateinit var rabbitSender: Sender
   lateinit var rabbitReceiver: Receiver

   val objectMapper = Jackson.newObjectMapperWithDefaults()

   @Before
   fun setup() {
      StepVerifier.setDefaultTimeout(Duration.ofSeconds(5))
      log().info("Admin user/pass for rabbitMQ: ${rabbitContainer.adminUsername} / ${rabbitContainer.adminPassword} http port: ${rabbitContainer.httpPort}")
      val connectionFactory = ConnectionFactory()
      connectionFactory.useNio()
      val address = Address(rabbitContainer.host, rabbitContainer.amqpPort)
      rabbitSender = RabbitAdmin.rabbitSender(connectionFactory, address)
      rabbitReceiver = RabbitAdmin.rabbitReceiver(connectionFactory, address)
      dispatcher = createDispatcher()

      dispatcher.setupRabbit()
         .block()
   }

   lateinit var dispatcher: RabbitMqQueueDispatcher

   @Test
   fun sendsQueryToQueue() {
      val payload = mapOf("message" to "Hello, world")
      val clientQueryId = Ids.id("query")
      dispatcher.dispatchQuery("find { Foo }", clientQueryId = clientQueryId)
         .test()
         .expectSubscription()
         .then {
            // read the query message from the queue
            consumeQueryMessage()
            sendResultToExchange(clientQueryId, payload)
         }
         .expectNext(payload)
         .verifyComplete()

   }

   @Test
   fun returnsStreamingResponses() {
      val dispatcher = createDispatcher()
      fun messagePayload(message: String) = mapOf("message" to message)
      val clientQueryId = Ids.id("query")
      dispatcher.dispatchQuery("find { Foo }", clientQueryId = clientQueryId)
         .test()
         .expectSubscription()
         .then {
            // read the query message from the queue
            consumeQueryMessage()
            sendStreamResultToResultQueue(clientQueryId, messagePayload("message1"))
         }
         .expectNext(messagePayload("message1"))
         .then { sendStreamResultToResultQueue(clientQueryId, messagePayload("message2")) }
         .expectNext(messagePayload("message2"))
         .then { sendStreamCompletedMessage(clientQueryId) }
         .verifyComplete()
   }

   @Test
   fun returnsErrorWhenQueryFails() {
      val dispatcher = createDispatcher()
      val clientQueryId = Ids.id("query")
      dispatcher.dispatchQuery("find { Foo }", clientQueryId = clientQueryId)
         .test()
         .expectSubscription()
         .then {
            // read the query message from the queue
            val queryMessage = consumeQueryMessage()
            sendErrorResultToQueue(clientQueryId, "The query failed")
         }
         .expectErrorMatches { error ->
            error is QueryFailedException
            error.message!!.shouldBe("The query failed")
            true
         }
         .verify()
   }

   private fun sendErrorResultToQueue(clientQueryId: String, error: String) {
      writeMessageToResponseExchange(
         clientQueryId,
         QueryResponseMessage(null, QueryResponseMessage.QueryResponseMessageKind.ERROR, error)
      )
   }

   private fun sendStreamCompletedMessage(clientQueryId: String) {
      writeMessageToResponseExchange(
         clientQueryId,
         QueryResponseMessage(
            null,
            QueryResponseMessage.QueryResponseMessageKind.END_OF_STREAM
         )
      )
   }

   private fun sendStreamResultToResultQueue(clientQueryId: String, messagePayload: Map<String, String>) {
      writeMessageToResponseExchange(
         clientQueryId,
         QueryResponseMessage(
            messagePayload,
            QueryResponseMessage.QueryResponseMessageKind.STREAM_MESSAGE
         )
      )
   }

   private fun sendResultToExchange(queryId: String, payload: Map<String, String>) {
      writeMessageToResponseExchange(
         queryId,
         QueryResponseMessage(
            payload,
            QueryResponseMessage.QueryResponseMessageKind.RESULT
         )
      )
   }

   private fun writeMessageToResponseExchange(
      key: String,
      message: QueryResponseMessage
   ) {
      rabbitSender.send(
         Mono.just(
            OutboundMessage(
               RESPONSES_EXCHANGE_NAME,
               key,
               objectMapper.writeValueAsBytes(message)
            )
         )
      ).block()
   }

   private fun consumeQueryMessage(): QueryMessage {
      // Without this sleep here, the consumer never receives the message.
      // I can't work out why.
      Thread.sleep(100)
      val messageDelivery = rabbitReceiver
         .consumeAutoAck(QUERIES_QUEUE_NAME)
         .subscribeOn(Schedulers.boundedElastic())
         .map { delivery ->
            log().info("Received query")
            delivery
         }
         .blockFirst()
      val wrapper = objectMapper.readValue<QueryMessageCborWrapper>(messageDelivery.body)
      return wrapper.message()
   }

   private fun createDispatcher(): RabbitMqQueueDispatcher {
      val schemaProvider = SimpleSchemaProvider(TaxiSchema.empty())
      return RabbitMqQueueDispatcher(
         rabbitSender,
         rabbitReceiver,
         ServicesConfigRepository(configRoot!!.root.resolve("services.conf").toPath()),
         EmptyAuthSchemeRepository,
          SourceLoaderConnectorsRegistry(configRoot!!.root.resolve("connections.conf").toPath()),
         schemaProvider,
      )
   }
}
