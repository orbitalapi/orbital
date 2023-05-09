package io.vyne.query.runtime.executor.rabbitmq

import io.vyne.query.runtime.core.dispatcher.rabbitmq.RabbitAdmin
import io.vyne.query.runtime.executor.StandaloneVyneFactory
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
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
      @Value("\${vyne.consumer.rabbit.concurrency:25}") concurrency: Int,
      @Value("\${vyne.consumer.rabbit.username:''}") rabbitUsername: String = "",
      @Value("\${vyne.consumer.rabbit.password:''}") rabbitPassword: String = "",
      vyneFactory: StandaloneVyneFactory,
   ): RabbitMqQueryExecutor? {

      // Can't use ConditionalOnProperty, as this isn't supported in AOT compilation for native images.
      // Instead, return null if support is not enabled.
      if (!enabled) {
         return null
      }

      logger.info { "Configuring RabbitMQ consumer at address $rabbitAddress with concurrency of $concurrency" }

      val (connectionFactory, addresses) = RabbitAdmin.newConnectionFactory(
         rabbitAddress,
         rabbitUsername,
         rabbitPassword
      )

      val sender = RabbitAdmin.rabbitSender(connectionFactory, addresses)
      val receiver = RabbitAdmin.rabbitReceiver(connectionFactory, addresses)

      val executor = RabbitMqQueryExecutor(sender, receiver, vyneFactory, parallelism = concurrency)
      RabbitAdmin.configureRabbit(executor.setupRabbit())
      return executor
   }
}
