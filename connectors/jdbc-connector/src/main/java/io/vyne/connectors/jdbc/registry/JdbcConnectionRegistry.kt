package io.vyne.connectors.jdbc.registry

import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.registry.ConnectionRegistry

interface JdbcConnectionRegistry : ConnectionRegistry<JdbcConnectionConfiguration> {
}
