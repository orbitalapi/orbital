package io.vyne.query.runtime.core.dispatcher.rabbitmq

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Address
import com.rabbitmq.client.ConnectionFactory
import io.vyne.auth.tokens.AuthTokenRepository
import io.vyne.connectors.config.ConfigFileConnectorsRegistry
import io.vyne.http.ServicesConfigRepository
import io.vyne.models.json.Jackson
import io.vyne.query.QueryFailedException
import io.vyne.query.QueryResponseMessage
import io.vyne.query.ResultMode
import io.vyne.query.runtime.QueryMessage
import io.vyne.query.runtime.QueryMessageCborWrapper
import io.vyne.query.runtime.core.dispatcher.StreamingQueryDispatcher
import io.vyne.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin.QUERIES_QUEUE_NAME
import io.vyne.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin.QUERY_EXCHANGE_NAME
import io.vyne.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin.RESPONSES_EXCHANGE_NAME
import io.vyne.schema.api.SchemaProvider
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
   private val authTokenRepository: AuthTokenRepository,
   private val connectionsConfigProvider: ConfigFileConnectorsRegistry,
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
      return createTemporaryQueue(replyQueueName, clientQueryId)
         .map { _ ->
            QueryMessage(
               query = query,
               sourcePackages = schemaProvider.schema.packages,
               connections = connectionsConfigProvider.load(),
               authTokens = authTokenRepository.getAllTokens(),
               services = servicesRepository.load(),
               resultMode, mediaType, clientQueryId,
               arguments,
               replyTo = replyQueueName
            )
         }
         .flatMap { sendMessageToQueue(it) }
         .flatMapMany { queryMessage -> consumeResponses(queryMessage) }
   }

   private fun consumeResponses(queryMessage: QueryMessage): Flux<Any> {
      return setupRabbit().flatMapMany { rabbitReceiver.consumeAutoAck(queryMessage.replyTo!!) }
         .map { messageDelivery ->
            objectMapper.readValue<QueryResponseMessage>(messageDelivery.body)
         }
         .handle { responseMessage, sink ->
            when (responseMessage.messageKind) {
               QueryResponseMessage.QueryResponseMessageKind.ERROR -> {
                  logger.info { "Query ${queryMessage.clientQueryId} failed with error ${responseMessage.message}" }
                  sink.error(QueryFailedException(responseMessage.message!!))
               }

               QueryResponseMessage.QueryResponseMessageKind.RESULT -> {
                  logger.info { "Query ${queryMessage.clientQueryId} received result" }
                  sink.next(responseMessage.payload!!)
                  sink.complete()
               }

               QueryResponseMessage.QueryResponseMessageKind.STREAM_MESSAGE -> {
                  logger.info { "Query ${queryMessage.clientQueryId} received result stream update" }
                  sink.next(responseMessage.payload!!)
               }

               QueryResponseMessage.QueryResponseMessageKind.END_OF_STREAM -> {
                  logger.info { "Query ${queryMessage.clientQueryId} has finished" }
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
      return rabbitSender.sendWithPublishConfirms(messagePublisher, SendOptions().trackReturned(true))
         .doOnRequest { logger.info { "Dispatching query ${queryMessage.clientQueryId} to exchange ${outboundMessage.exchange} with key ${outboundMessage.routingKey}" } }
         .filter { publicationResult ->
            if (publicationResult.isAck && !publicationResult.isReturned) {
               logger.debug { "Query ${queryMessage.clientQueryId} dispatched successfully" }
            } else {
               logger.warn { "Failed to dispatch query ${queryMessage.clientQueryId}" }
            }
            if (publicationResult.isReturned) {
               logger.warn { "Query message ${queryMessage.clientQueryId} was returned by the broker because there are no configured destinations" }
            }
            publicationResult.isAck && !publicationResult.isReturned
         }
         .switchIfEmpty { subscriber ->
            subscriber.onError(QueryFailedException("Message failed to be delivered to any consumers before the message timed out.  "))
         }
         .single()
         .map { _ -> queryMessage }
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
            )
         )

   }


}
