package io.vyne.query.runtime.core.dispatcher.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Address
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Method
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.rabbitmq.*
import reactor.util.retry.Retry
import java.time.Duration

object RabbitAdmin {
   private val logger = KotlinLogging.logger {}
   const val RESPONSES_EXCHANGE_NAME = "responses-exchange"
   const val QUERIES_QUEUE_NAME = "queries"
   const val QUERY_EXCHANGE_NAME = "queries-exchange"

   fun rabbitReceiver(connectionFactory: ConnectionFactory, vararg rabbitMqAddress: Address): Receiver {
      val receiverOptions = ReceiverOptions()
         .connectionFactory(connectionFactory)
         .connectionSupplier { connectionFactory ->
            connectionFactory.newConnection(rabbitMqAddress)
         }
      return RabbitFlux.createReceiver(receiverOptions)
   }

   fun rabbitSender(connectionFactory: ConnectionFactory, vararg rabbitMqAddress: Address): Sender {
      val senderOptions = SenderOptions()
         .connectionFactory(connectionFactory)
         .connectionSupplier { connectionFactory ->
            connectionFactory.newConnection(rabbitMqAddress)
         }
         .resourceManagementScheduler(Schedulers.boundedElastic())
      return RabbitFlux.createSender(senderOptions)
   }

   fun replyQueueName(queryId: String) = "query-$queryId-responses"

   fun createQueriesExchangeAndQueue(sender: Sender): Mono<AMQP.Queue.BindOk> {
      return createExchangeAndQueue(
         QUERY_EXCHANGE_NAME,
         BuiltinExchangeType.DIRECT,
         QUERIES_QUEUE_NAME,
         sender = sender,
         exchangeArgs = mapOf("x-message-ttl" to 5000),
         queueArgs = mapOf("x-message-ttl" to 5000),
      )
   }

   fun createResponsesExchange(sender: Sender): Mono<AMQP.Exchange.DeclareOk> {
      return sender.declare(
         ExchangeSpecification.exchange(RESPONSES_EXCHANGE_NAME)
            .type(BuiltinExchangeType.DIRECT.type)
            .autoDelete(false)
            .durable(false)
      )
   }

   fun createExchangeAndQueue(
      exchangeName: String,
      exchangeType: BuiltinExchangeType,
      queueName: String,
      routingKey: String = queueName,
      sender: Sender,
      exchangeArgs: Map<String, Any> = emptyMap(),
      queueArgs: Map<String, Any> = emptyMap(),
   ): Mono<AMQP.Queue.BindOk> {
      return sender.declare(
         ExchangeSpecification.exchange(exchangeName)
            .type(exchangeType.type)
            .arguments(exchangeArgs)
            .autoDelete(false)
            .durable(true)
      ).doOnRequest { logger.info { "Creating $exchangeType exchange named $exchangeName" } }
         .then(sender.declareQueue(QueueSpecification.queue(queueName).arguments(queueArgs))
            .doOnRequest { logger.info { "Creating queue $queueName" } }
         )
         .then(sender.bindQueue(BindingSpecification.queueBinding(exchangeName, routingKey, queueName))
            .doOnRequest { logger.info { "Binding exchange $exchangeName to queue $queueName with routing key $routingKey" } }
         )
   }

   fun createQueueAndBindToExchange(
      queueName: String,
      exchangeName: String,
      routingKey: String,
      temporary: Boolean,
      sender: Sender
   ): Mono<AMQP.Queue.BindOk> {
      return sender.declareQueue(QueueSpecification.queue(queueName).autoDelete(temporary))
         .doOnRequest { logger.info { "Creating queue $queueName" } }
         .then(sender.bindQueue(BindingSpecification.queueBinding(exchangeName, routingKey, queueName)))
         .doOnRequest { logger.info { "Binding exchange $exchangeName to queue $queueName with routing key $routingKey" } }
   }

   fun configureRabbit(declaration: Mono<out Method>) {
      declaration
         .doOnError { error ->
            logger.info { "Failed initialize RabbitMQ - ${error::class.simpleName} - ${error.message ?: "No message"}. Continuing to retry" }
         }
         .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(10)))
         .onErrorComplete { error ->
            logger.error(error) { "Failed to initialize RabbitMQ queues and exchanges.  Will not continue to retry" }
            true
         }
         .subscribe { result ->
            logger.info { "Rabbit queues and exchanges completed" }
         }
   }
}
