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
import io.vyne.query.runtime.executor.QueryExecutor
import io.vyne.utils.withQueryId
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux
import reactor.core.scheduler.Schedulers
import reactor.rabbitmq.*
import java.time.Duration

/**
 * Subscribes to a RabbitMQ queue for queries, and executes a query
 */
class RabbitMqQueryExecutor(
   private val rabbitSender: Sender,
   private val rabbitReceiver: Receiver,
//   private val vyneFactory: StandaloneVyneFactory,
   private val queryExecutor: QueryExecutor,
   private val objectMapper: ObjectMapper = Jackson.newObjectMapperWithDefaults(),
   private val parallelism: Int = 1,
   private val subscribeForNewQueries: Boolean = true
) {
   init {
      require(parallelism > 0) { "Parallelism must be greater than 0" }
      logger.info { "RabbitMQ Executor running with parallelism of $parallelism" }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @PostConstruct
   fun onStart() {
      if (!subscribeForNewQueries) {
         logger.info { "RabbitMQ Consumer is not subscribing for new queries - execution needs to be triggered manually" }
         return
      }
      logger.info { "Starting to consume queries from RabbitMQ" }
      consumeAndExecuteQueries()
         .subscribeOn(Schedulers.boundedElastic())
         .subscribe { (messageKind, event) ->
            when {
               !event.isAck -> logger.withQueryId(event.outboundMessage.routingKey)
                  .warn { "Received a NAck for ${messageKind.name} message being sent to query ${event.outboundMessage.routingKey}" }

               event.isAck && event.isReturned -> logger.withQueryId(event.outboundMessage.routingKey)
                  .warn { "${messageKind.name} message being sent to query ${event.outboundMessage.routingKey} was returned, as no destinations exist." }

               else -> logger.withQueryId(event.outboundMessage.routingKey)
                  .debug { "${messageKind.name} message for query ${event.outboundMessage.routingKey} was delivered to the exchange successfully" }
            }
         }
   }

   /**
    * Returns a flux of outbound messages relating to received queries.
    *
    * As this method continually consumes query messages,
    * the returned flux never completes.
    */
   fun consumeAndExecuteQueries(): Flux<Pair<QueryResponseMessage.QueryResponseMessageKind, OutboundMessageResult<OutboundMessage>>> {
      return setupRabbit()
         .publishOn(Schedulers.boundedElastic())
         .flatMapMany {
            consumeQueries()
               .doOnRequest {
                  logger.info { "Downstream consumer has requested $it new messages" }
               }
               .flatMap(
                  { queryMessage -> executeQueryAndWriteResponsesToRabbit(queryMessage) },
                  false,
                  parallelism
               ) // Specifying the parallelism here defines the rate at which flatMap is performed.
         }
   }

   fun executeQueryAndWriteResponsesToRabbit(queryMessage: QueryMessage): Flux<Pair<QueryResponseMessage.QueryResponseMessageKind, OutboundMessageResult<OutboundMessage>>> =
      executeQuery(queryMessage)
         // This flatMap consumes the results from the query, and sends them to Rabbit.
         // Keeping it inside the parent flatMap() ensures we can control the rate of consumption from
         // rabbit.

         .flatMap { (messageKind, outboundMessage) ->
            // Write the response to Rabbit.
            // We capture the first reciept - in theory there should be only one
            writeResultToRabbitMq(outboundMessage)
               .publishOn(Schedulers.boundedElastic())
               .map { outboundMessageResult -> messageKind to outboundMessageResult }
               .single()
               .timeout(Duration.ofSeconds(10))
               .doOnError {
                  logger.withQueryId(outboundMessage.routingKey)
                     .error { "Rabbit failed to ACK a response message" }
               }
         }


   private fun consumeQueries(): ParallelFlux<QueryMessage> {
      val queryFlux = rabbitReceiver.consumeAutoAck(RabbitAdmin.QUERIES_QUEUE_NAME, ConsumeOptions().qos(parallelism))
         .limitRate(parallelism)
         .mapNotNull { delivery ->
            logger.info { "Received new query message" }
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

//      return queryFlux as Flux<QueryMessage>
      return if (parallelism > 1) {
         queryFlux.runOn(Schedulers.parallel())
      } else {
         queryFlux
      }
   }

   private fun streamEndedMessage(queryMessage: QueryMessage): Mono<Pair<QueryResponseMessage.QueryResponseMessageKind, OutboundMessage>> {
      return Mono.defer {
         logger.info { "Query ${queryMessage.clientQueryId} has completed, sending stream end message" }
         val responseMessage = QueryResponseMessage.COMPLETED
         val messageBytes = objectMapper.writeValueAsBytes(responseMessage)
         Mono.just(
            responseMessage.messageKind to
               OutboundMessage(
                  RabbitAdmin.RESPONSES_EXCHANGE_NAME,
                  queryMessage.clientQueryId,
                  messageBytes
               )
         )
      }

   }

   private fun writeResultToRabbitMq(
      message: OutboundMessage,
   ): Flux<OutboundMessageResult<OutboundMessage>> {
      return rabbitSender.sendWithPublishConfirms(
         Mono.just(message)
      )
   }

   private fun executeQuery(queryMessage: QueryMessage): Flux<Pair<QueryResponseMessage.QueryResponseMessageKind, OutboundMessage>> {
      return queryExecutor.executeQuery(queryMessage)
         .subscribeOn(Schedulers.boundedElastic())
         .map { result ->
            logger.debug { "Query ${queryMessage.clientQueryId} emitting result" }
            QueryResponseMessage.streamResult(result)
         }
         .onErrorResume { error ->
            logger.withQueryId(queryMessage.clientQueryId).error(error) { "An error occurred in processing the query" }
            Mono.just(
               QueryResponseMessage.error(
                  error.message ?: "An unknown error occurred, with error type ${error::class.simpleName}"
               )
            )
         }
         .map { queryResponseMessage ->
            queryResponseMessage.messageKind to wrapInRabbitMessage(queryMessage, queryResponseMessage)
         }
         // When the query is finished, send a stream ended message.
         // concatMap() didn't work here - only the termination message (ie., the
         // second flux)
         // was sent.  Needs to be concatWith().
         .concatWith(streamEndedMessage(queryMessage))
   }

   private fun wrapInRabbitMessage(
      message: QueryMessage,
      queryResponseMessage: QueryResponseMessage
   ) = OutboundMessage(
      RabbitAdmin.RESPONSES_EXCHANGE_NAME,
      message.clientQueryId,
      objectMapper.writeValueAsBytes(queryResponseMessage),
   )

   @VisibleForTesting
   internal fun setupRabbit(): Mono<AMQP.Exchange.DeclareOk> {
      return RabbitAdmin.createQueriesExchangeAndQueue(rabbitSender)
         .then(RabbitAdmin.createResponsesExchange(rabbitSender))
         .cache()
   }

}


