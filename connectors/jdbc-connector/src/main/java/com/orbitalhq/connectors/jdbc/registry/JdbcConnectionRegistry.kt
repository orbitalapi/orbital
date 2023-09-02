package com.orbitalhq.connectors.jdbc.registry

import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectionRegistry

interface JdbcConnectionRegistry : ConnectionRegistry<JdbcConnectionConfiguration> {
}
