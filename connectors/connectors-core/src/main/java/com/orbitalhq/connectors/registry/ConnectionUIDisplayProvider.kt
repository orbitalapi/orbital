package com.orbitalhq.connectors.registry

interface ConnectionUIDisplayProvider {
   fun uiProps(connectorConfig: ConnectorConfiguration): Map<String, Any>?
}
