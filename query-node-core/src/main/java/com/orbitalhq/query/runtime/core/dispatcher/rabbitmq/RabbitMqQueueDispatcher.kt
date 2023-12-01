package com.orbitalhq.query.runtime.core.dispatcher.rabbitmq

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.AMQP
import com.orbitalhq.auth.schemes.AuthSchemeRepository
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryFailedException
import com.orbitalhq.query.QueryResponseMessage
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.query.runtime.QueryMessageCborWrapper
import com.orbitalhq.query.runtime.core.dispatcher.StreamingQueryDispatcher
import com.orbitalhq.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin.QUERIES_QUEUE_NAME
import com.orbitalhq.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin.QUERY_EXCHANGE_NAME
import com.orbitalhq.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin.RESPONSES_EXCHANGE_NAME
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.utils.withQueryId
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.rabbitmq.*

/**
 * Sends queries onto a RabbitMQ queue
 */
class RabbitMqQueueDispatcher(
    private val rabbitSender: Sender,
    private val rabbitReceiver: Receiver,
    private val servicesRepository: ServicesConfigRepository,
    private val authTokenRepository: AuthSchemeRepository,
    private val connectionsConfigProvider: SourceLoaderConnectorsRegistry,
    private val schemaProvider: SchemaProvider,
    private val objectMapper: ObjectMapper = Jackson.newObjectMapperWithDefaults(),
) : StreamingQueryDispatcher {

   init {
      logger.info { "RabbitMQ Dispatcher is active" }
   }

   companion object {

      private val logger = KotlinLogging.logger {}


   }

   fun setupRabbit(): Mono<AMQP.Exchange.DeclareOk> {
      return RabbitAdmin.createQueriesExchangeAndQueue(rabbitSender)
         .then(RabbitAdmin.createResponsesExchange(rabbitSender))
   }

   override fun dispatchQuery(
      query: String,
      clientQueryId: String,
      mediaType: String,
      resultMode: ResultMode,
      arguments: Map<String, Any?>
   ): Flux<Any> {

      val replyQueueName = RabbitAdmin.replyQueueName(clientQueryId)
      return Mono.just(replyQueueName)
         .publishOn(Schedulers.boundedElastic())
         .flatMap { createTemporaryQueue(replyQueueName, clientQueryId) }
         .map { _ ->
            QueryMessage(
               query = query,
               sourcePackages = schemaProvider.schema.packages,
               connections = connectionsConfigProvider.load(),
               authTokens = authTokenRepository.getAllTokens(),
               services = servicesRepository.loadConfig(),
               resultMode, mediaType, clientQueryId,
               arguments,
               replyTo = replyQueueName
            )
         }
         .flatMap { sendMessageToQueue(it) }
         .flatMapMany { queryMessage ->
            consumeResponses(queryMessage)
         }
   }

   private fun consumeResponses(queryMessage: QueryMessage): Flux<Any> {
      return rabbitReceiver.consumeAutoAck(queryMessage.replyTo!!)
         .map { messageDelivery ->
            logger.withQueryId(queryMessage.clientQueryId).debug { "Inbound message for query ${queryMessage.clientQueryId} received" }
            objectMapper.readValue<QueryResponseMessage>(messageDelivery.body)
         }
         .handle { responseMessage, sink ->
            when (responseMessage.messageKind) {
               QueryResponseMessage.QueryResponseMessageKind.ERROR -> {
                  logger.withQueryId(queryMessage.clientQueryId).info { "Query ${queryMessage.clientQueryId} failed with error ${responseMessage.message}" }
                  sink.error(QueryFailedException(responseMessage.message!!))
               }

               QueryResponseMessage.QueryResponseMessageKind.RESULT -> {
                  logger.withQueryId(queryMessage.clientQueryId).info { "Query ${queryMessage.clientQueryId} received result" }
                  sink.next(responseMessage.payload!!)
                  sink.complete()
               }

               QueryResponseMessage.QueryResponseMessageKind.STREAM_MESSAGE -> {
                  logger.withQueryId(queryMessage.clientQueryId).info { "Query ${queryMessage.clientQueryId} received result stream update" }
                  sink.next(responseMessage.payload!!)
               }

               QueryResponseMessage.QueryResponseMessageKind.END_OF_STREAM -> {
                  logger.withQueryId(queryMessage.clientQueryId).info { "Query ${queryMessage.clientQueryId} has finished" }
                  sink.complete()
               }
            }
         }
   }

   private fun sendMessageToQueue(queryMessage: QueryMessage): Mono<QueryMessage> {
      val message = QueryMessageCborWrapper.from(queryMessage)
      val jsonMessage = objectMapper.writeValueAsBytes(message)
      val outboundMessage = OutboundMessage(QUERY_EXCHANGE_NAME, QUERIES_QUEUE_NAME, jsonMessage)
      val messagePublisher = Mono.just(outboundMessage)
      return rabbitSender.send(messagePublisher)
         .then(Mono.fromCallable { queryMessage })
   }

   private fun createTemporaryQueue(queueName: String, queryId: String): Mono<AMQP.Queue.BindOk> {
      return rabbitSender.declareQueue(QueueSpecification.queue(queueName).durable(false).autoDelete(true))
         .doOnRequest {
            logger.info { "Creating reply queue for query $queryId named $queueName" }
         }
         .then(
            rabbitSender.bindQueue(
               BindingSpecification.queueBinding(
                  RESPONSES_EXCHANGE_NAME,
                  queryId,
                  queueName
               )
            ).doOnRequest { logger.info { "Binding temporary reply queue for $queryId to $queueName for exchange ${RabbitAdmin.RESPONSES_EXCHANGE_NAME}" } }
         )

   }


}
