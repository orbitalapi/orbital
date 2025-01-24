package com.orbitalhq.connectors.azure.servicebus.registry

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.ResultWithMessage
import com.orbitalhq.connectors.config.azure.ServiceBusConnectionConfiguration
import com.orbitalhq.connectors.registry.MutableConnectionRegistry

class InMemoryServiceBusConnectionRegistry(configs: List<ServiceBusConnectionConfiguration> = emptyList()): ServiceBusConnectionRegistry,
    MutableConnectionRegistry<ServiceBusConnectionConfiguration> {

    private val connections: MutableMap<String, ServiceBusConnectionConfiguration> =
        configs.associateBy { it.connectionName }.toMutableMap()
    override fun register(
        targetPackage: PackageIdentifier,
        connectionConfiguration: ServiceBusConnectionConfiguration
    ): ResultWithMessage {
        connections[connectionConfiguration.connectionName] = connectionConfiguration
        return ResultWithMessage.SUCCESS
    }

    override fun remove(targetPackage: PackageIdentifier, connectionName: String): ResultWithMessage {
        connections.remove(connectionName)
        return ResultWithMessage.SUCCESS

    }

    override fun hasConnection(name: String): Boolean = connections.containsKey(name)

    override fun getConnection(name: String): ServiceBusConnectionConfiguration = connections[name] ?: error("No ServiceBus Connection with name $name is registered")

    override fun listAll(): List<ServiceBusConnectionConfiguration> = this.connections.values.toList()
}