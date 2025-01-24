package com.orbitalhq.connectors.azure.servicebus.registry

import com.orbitalhq.connectors.config.ConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.azure.ServiceBusConnectionConfiguration
import com.orbitalhq.connectors.registry.MutableConnectionRegistry
import com.orbitalhq.connectors.registry.SourceLoaderConnectionRegistryAdapter

class SourceLoaderServiceBusConnectionRegistry(sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry): ServiceBusConnectionRegistry,
    MutableConnectionRegistry<ServiceBusConnectionConfiguration>,
    SourceLoaderConnectionRegistryAdapter<ServiceBusConnectionConfiguration>(
        sourceLoaderConnectorsRegistry,
        ConnectionsConfig::serviceBus
    )