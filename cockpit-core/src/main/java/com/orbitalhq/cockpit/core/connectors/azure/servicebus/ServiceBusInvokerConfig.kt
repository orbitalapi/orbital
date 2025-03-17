package com.orbitalhq.cockpit.core.connectors.azure.servicebus

import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.azure.servicebus.ServiceBusConnectionFactory
import com.orbitalhq.connectors.azure.servicebus.ServiceBusInvoker
import com.orbitalhq.connectors.azure.servicebus.ServiceBusPublisher
import com.orbitalhq.connectors.azure.servicebus.ServiceBusReceiver
import com.orbitalhq.connectors.azure.servicebus.registry.ServiceBusConnectionRegistry
import com.orbitalhq.connectors.azure.servicebus.registry.SourceLoaderServiceBusConnectionRegistry
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.schema.api.SchemaProvider
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class ServiceBusInvokerConfig {
    @Bean
    fun serviceBusConnectionRegistry(config: SourceLoaderConnectorsRegistry): ServiceBusConnectionRegistry {
        return SourceLoaderServiceBusConnectionRegistry(config)
    }

    @Bean
    fun serviceBusConnectionFactory(serviceBusConnectionRegistry: ServiceBusConnectionRegistry): ServiceBusConnectionFactory {
        return ServiceBusConnectionFactory(serviceBusConnectionRegistry)
    }

    @Bean
    fun serviceBusInvoker(
                          schemaProvider: SchemaProvider,
                          serviceBusPublisher: ServiceBusPublisher,
                          serviceBusReceiver: ServiceBusReceiver): ServiceBusInvoker {
        return ServiceBusInvoker(schemaProvider, serviceBusPublisher, serviceBusReceiver)
    }

    @Bean
    fun serviceBusPublisher(serviceBusConnectionFactory: ServiceBusConnectionFactory,
                            formatRegistry: FormatRegistry,
                            meterRegistry: MeterRegistry
    ) : ServiceBusPublisher {
        return ServiceBusPublisher(serviceBusConnectionFactory, formatRegistry, meterRegistry)
    }

    @Bean
    fun serviceBusReceiver(serviceBusConnectionFactory: ServiceBusConnectionFactory,
                           formatRegistry: FormatRegistry,
                           meterRegistry: MeterRegistry): ServiceBusReceiver {
        return ServiceBusReceiver(serviceBusConnectionFactory, formatRegistry, meterRegistry)
    }
}