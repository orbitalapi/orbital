package com.orbitalhq.query.runtime.core.dispatcher.rabbitmq

import com.orbitalhq.auth.schemes.AuthSchemeRepository
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.schema.api.SchemaProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty("vyne.dispatcher.rabbit.enabled", havingValue = "true", matchIfMissing = false)
class RabbitDispatcherConfig {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @Bean
   fun rabbitDispatcher(
       @Value("\${vyne.dispatcher.rabbit.address}") rabbitAddress: String,
       servicesRepository: ServicesConfigRepository,
       authTokenRepository: AuthSchemeRepository,
       connectionsConfigProvider: SourceLoaderConnectorsRegistry,
       schemaProvider: SchemaProvider,
       @Value("\${vyne.dispatcher.rabbit.username:''}") rabbitUsername: String = "",
       @Value("\${vyne.dispatcher.rabbit.password:''}") rabbitPassword: String = "",
   ): RabbitMqQueueDispatcher {

      val (connectionFactory, addresses) = RabbitAdmin.newConnectionFactory(
         rabbitAddress,
         rabbitUsername,
         rabbitPassword
      )

      val reciever = RabbitAdmin.rabbitReceiver(connectionFactory, addresses)
      val sender = RabbitAdmin.rabbitSender(connectionFactory, addresses)

      val dispatcher = RabbitMqQueueDispatcher(
         sender,
         reciever,
         servicesRepository, authTokenRepository, connectionsConfigProvider, schemaProvider
      )
      logger.info { "Preparing queues and exchanges on Rabbit" }
      RabbitAdmin.configureRabbit(dispatcher.setupRabbit())

      return dispatcher
   }
}
