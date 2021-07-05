package io.vyne.connectors

interface Connector {
   val name: String
   val driver: String
   val address: String
   val type: ConnectorType
}

enum class ConnectorType {
   JDBC,
   KAFKA
}

/**
 * This is really a DTO.  Connectors can have lots of properties, and we only want to expose
 * the basic ones to the Web UI.
 */
data class ConnectorSummary(private val connector: Connector) : Connector by connector
