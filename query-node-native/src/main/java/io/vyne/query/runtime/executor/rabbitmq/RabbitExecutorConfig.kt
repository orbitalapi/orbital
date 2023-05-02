package io.vyne.query.runtime.executor.rabbitmq

import com.rabbitmq.client.Address
import com.rabbitmq.client.ConnectionFactory
import io.vyne.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin
import io.vyne.query.runtime.executor.StandaloneVyneFactory
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
// Can't use ConditionalOnProperty, as this isn't supported in AOT compilation for native images.
//@ConditionalOnProperty("vyne.consumer.rabbit.enabled", havingValue = "true", matchIfMissing = false)
class RabbitExecutorConfig {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @Bean
   fun rabbitExecutor(
      @Value("\${vyne.consumer.rabbit.enabled:false}") enabled: Boolean,
      @Value("\${vyne.consumer.rabbit.address}") rabbitAddress: String,
      @Value("\${vyne.consumer.rabbit.concurrency:1}") concurrency: Int,
      vyneFactory: StandaloneVyneFactory,
   ): RabbitMqQueryExecutor? {

      // Can't use ConditionalOnProperty, as this isn't supported in AOT compilation for native images.
      // Instead, return null if support is not enabled.
      if (!enabled) {
         return null
      }
      logger.info { "Configuring RabbitMQ consumer at address $rabbitAddress with concurrency of $concurrency" }
      val address = Address.parseAddresses(rabbitAddress)
      val connectionFactory = ConnectionFactory()
      connectionFactory.useNio()
      val sender = RabbitAdmin.rabbitSender(connectionFactory, *address)
      val receiver = RabbitAdmin.rabbitReceiver(connectionFactory, *address)

      val executor = RabbitMqQueryExecutor(sender, receiver, vyneFactory, parallelism = concurrency)
      RabbitAdmin.configureRabbit(executor.setupRabbit())
      return executor
   }
}
