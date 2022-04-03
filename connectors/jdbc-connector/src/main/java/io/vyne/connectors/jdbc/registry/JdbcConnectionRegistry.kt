package io.vyne.connectors.jdbc.registry

import io.vyne.connectors.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.registry.ConnectionRegistry

interface JdbcConnectionRegistry : ConnectionRegistry<JdbcConnectionConfiguration> {
}
