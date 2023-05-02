package io.vyne.query.runtime.executor.rabbitmq

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.annotations.VisibleForTesting
import com.rabbitmq.client.AMQP
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponseMessage
import io.vyne.query.runtime.QueryMessage
import io.vyne.query.runtime.QueryMessageCborWrapper
import io.vyne.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin
import io.vyne.query.runtime.executor.StandaloneVyneFactory
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.CorePublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux
import reactor.core.scheduler.Schedulers
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.OutboundMessageResult
import reactor.rabbitmq.Receiver
import reactor.rabbitmq.Sender
import kotlin.coroutines.CoroutineContext

/**
 * Subscribes to a RabbitMQ queue for queries, and executes a query
 */
class RabbitMqQueryExecutor(
   private val rabbitSender: Sender,
   private val rabbitReceiver: Receiver,
   private val vyneFactory: StandaloneVyneFactory,
   private val objectMapper: ObjectMapper = Jackson.newObjectMapperWithDefaults(),
   private val parallelism: Int = 1
) {
   init {
      require(parallelism > 0) { "Parallelism must be greater than 0" }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @PostConstruct
   fun onStart() {
      logger.info { "Starting to consume queries from RabbitMQ" }
      consumeAndExecuteQueries()
         .subscribeOn(Schedulers.boundedElastic())
         .subscribe { event ->
            when {
               !event.isAck -> logger.warn { "Received a NAck for response message being sent to query ${event.outboundMessage.routingKey}" }
               event.isAck && event.isReturned -> logger.warn { "Response message being sent to query ${event.outboundMessage.routingKey} was returned, as no destinations exist." }
               else -> logger.debug { "Response message for query ${event.outboundMessage.routingKey} was delivered to the exchange successfully" }
            }
         }
   }

   /**
    * Returns a flux of outbound messages relating to received queries.
    *
    * As this method continually consumes query messages,
    * the returned flux never completes.
    */
   fun consumeAndExecuteQueries(): Flux<OutboundMessageResult<OutboundMessage>> {
      return setupRabbit().flatMapMany {
         consumeQueries()
            .flatMap { queryMessage -> executeQuery(queryMessage!!) }
            .flatMap {
               // Write the response to Rabbit.
               // We capture the first reciept - in theory there should be only one
               writeResultToRabbitMq(it).single()
            }
      }
   }


   private fun consumeQueries(): ParallelFlux<QueryMessage> {
      val queryFlux = rabbitReceiver.consumeAutoAck(RabbitAdmin.QUERIES_QUEUE_NAME)
         .mapNotNull { delivery ->
            logger.debug { "Received new query message" }
            try {
               val wrapper = objectMapper.readValue<QueryMessageCborWrapper>(delivery.body)
               val message = wrapper.message()
               logger.debug { "Read query ${message.clientQueryId}" }
               message
            } catch (e: Exception) {
               logger.error(e) { "Failed to read inbound query message. Ignoring this message so processing can continue" }
               null
            }
         }
         .parallel(parallelism) as ParallelFlux<QueryMessage>

      return if (parallelism > 1) {
         queryFlux.runOn(Schedulers.parallel())
      } else {
         queryFlux
      }
   }

   private fun streamEndedMessage(queryMessage: QueryMessage): Mono<OutboundMessage> {
      logger.info { "Query ${queryMessage.clientQueryId} has completed, sending stream end message" }
      val messageBytes = objectMapper.writeValueAsBytes(QueryResponseMessage.COMPLETED)
      return Mono.just(
         OutboundMessage(
            RabbitAdmin.RESPONSES_EXCHANGE_NAME,
            queryMessage.clientQueryId,
            messageBytes
         )
      )
   }

   private fun writeResultToRabbitMq(
      message: OutboundMessage
   ): Flux<OutboundMessageResult<OutboundMessage>> {
      return rabbitSender.sendWithPublishConfirms(
         Mono.just(message)
      )
   }

   private fun executeQuery(queryMessage: QueryMessage): Flux<OutboundMessage> {
      val vyne = vyneFactory.buildVyne(queryMessage)
      val args = queryMessage.args()

      logger.info { "Initializing query execution for new query ${queryMessage.clientQueryId}" }

      // Runblocking here is misleading - the Vyne API is wrong.
      // The suspend actually happens in the consumption of the results flow, not
      // in the construction of the response.
      val flow = runBlocking {
         vyne.query(
            queryMessage.query,
            clientQueryId = queryMessage.clientQueryId,
            arguments = args
         ).results
      }

      return flow.asFlux()
         .map { result ->
            logger.debug { "Query ${queryMessage.clientQueryId} emitting result" }
            QueryResponseMessage.streamResult(result.toRawObject())
         }
         .onErrorResume { error ->
            Mono.just(
               QueryResponseMessage.error(
                  error.message ?: "An unknown error occurred, with error type ${error::class.simpleName}"
               )
            )
         }
         .map { queryResponseMessage ->
            wrapInRabbitMessage(queryMessage, queryResponseMessage)
         }
         // When the query is finished, send a stream ended message.
         // concatMap() didn't work here - only the termination message (ie., the
         // second flux)
         // was sent.  Needs to be concatWith().
         .concatWith(streamEndedMessage(queryMessage))
   }

   private fun wrapInRabbitMessage(
      message: QueryMessage,
      queryResponseMessage: QueryResponseMessage?
   ) = OutboundMessage(
      RabbitAdmin.RESPONSES_EXCHANGE_NAME,
      message.clientQueryId,
      objectMapper.writeValueAsBytes(queryResponseMessage)
   )

   @VisibleForTesting
   internal fun setupRabbit(): Mono<AMQP.Exchange.DeclareOk> {
      return RabbitAdmin.createQueriesExchangeAndQueue(rabbitSender)
         .then(RabbitAdmin.createResponsesExchange(rabbitSender))
   }

}


