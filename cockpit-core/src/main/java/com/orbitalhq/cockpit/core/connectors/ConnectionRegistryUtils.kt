package com.orbitalhq.cockpit.core.connectors

import com.orbitalhq.connectors.registry.ConnectionRegistry
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.spring.http.NotFoundException

fun <T : ConnectorConfiguration> ConnectionRegistry<T>.getConnectionConfigOrThrowNotFound(connectionName: String): T {
   if (!this.hasConnection(connectionName)) {
      throw NotFoundException("No connection named $connectionName is defined")
   }
   val connection = this.getConnection(connectionName)
   return connection
}
