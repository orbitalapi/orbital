package io.vyne.connectors.registry

import io.vyne.connectors.config.SourceLoaderConnectorsRegistry
import io.vyne.connectors.config.ConnectionsConfig

interface ConnectionRegistry<T : ConnectorConfiguration> {
    fun hasConnection(name: String): Boolean
    fun getConnection(name: String): T
    fun listAll(): List<T>
}

interface MutableConnectionRegistry<T : ConnectorConfiguration> : ConnectionRegistry<T> {
    fun register(connectionConfiguration: T)
    fun remove(connectionConfiguration: T)
}

/**
 * An adaptor between the new ConfigFileConnectorsRegistry / Loader approach,
 * and the legacy XxxConnectionRegistry approach.
 *
 * ConfigFileConnectorsRegistry supports reloading from sources like Schemas, and
 * is the preferred way for loading config.  However, ConnectionRegistry<> are everywhere.
 *
 * This lets us quickly adapt
 */
abstract class SourceLoaderConnectionRegistryAdapter<T : ConnectorConfiguration>(
    private val sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry,
    private val selector: (ConnectionsConfig) -> Map<String, T>
) : ConnectionRegistry<T> {
    private fun getCurrent(): Map<String, T> {
        return selector.invoke(sourceLoaderConnectorsRegistry.load())
    }

    override fun getConnection(name: String): T {
        return getCurrent().getValue(name)
    }

    override fun hasConnection(name: String): Boolean {
        return getCurrent().containsKey(name)
    }

    override fun listAll(): List<T> {
        return getCurrent().values.toList()
    }
}
