package com.orbitalhq.connectors.azure.servicebus

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient
import com.google.common.cache.CacheBuilder
import com.orbitalhq.config.UpdatableConfigRepository
import com.orbitalhq.connectors.azure.servicebus.registry.ServiceBusConnectionRegistry
import com.orbitalhq.connectors.config.azure.ServiceBusConnectionConfiguration
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {  }
class ServiceBusConnectionFactory(private val serviceBusConnectionRegistry: ServiceBusConnectionRegistry) {

    private val clientBuilderCache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .removalListener<String, ServiceBusClientBuilder> {
            logger.warn { "Disposing the ServiceBusClientBuilder for ${it.key}" }
        }
        .build<String, ServiceBusClientBuilder>()
    init {
        if (serviceBusConnectionRegistry is UpdatableConfigRepository<*>) {
            serviceBusConnectionRegistry.configUpdated.subscribe {
                logger.info { "Connection registry changed - invalidating cache of ServiceBus connections" }
                clientBuilderCache.invalidateAll()
            }
        }
    }

    fun healthCheck(connection: ServiceBusConnectionConfiguration) {
       serviceBusClientBuilder(connection.connectionName)

    }



    private fun serviceBusClientBuilder(connectionName: String): ServiceBusClientBuilder {
        val connectionString = serviceBusConnectionRegistry.getConnection(connectionName).connectionString
        return clientBuilderCache.get(connectionString) {
            ServiceBusClientBuilder().connectionString(connectionString)
        }
    }

    fun queueSender(connectionName: String, queueName: String): ServiceBusSenderAsyncClient {
       return serviceBusClientBuilder(connectionName)
           .sender()
           .queueName(queueName)
           .buildAsyncClient()
    }

    fun queueReceiver(connectionName: String, queueName: String): ServiceBusReceiverAsyncClient {
        return serviceBusClientBuilder(connectionName)
            .receiver()
            .queueName(queueName)
            .buildAsyncClient()
    }

    fun topicSender(connectionName: String, topicName: String): ServiceBusSenderAsyncClient {
        return serviceBusClientBuilder(connectionName)
            .sender()
            .topicName(topicName)
            .buildAsyncClient()
    }

    fun topicReceiver(connectionName: String, topicName: String, subscriptionName: String): ServiceBusReceiverAsyncClient {
        return serviceBusClientBuilder(connectionName)
            .receiver()
            .topicName(topicName)
            .subscriptionName(subscriptionName)
            .buildAsyncClient()
    }

    fun serviceBusConnection(connectionName: String) = serviceBusConnectionRegistry.getConnection(connectionName)
}