package io.vyne.connectors.config.gcp

import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType
import kotlinx.serialization.Serializable
import java.nio.file.Path

object GcpConnection {
   const val DRIVER_NAME = "GcpConnection"
}

@Serializable
data class GcpConnectionConfiguration(
   override val connectionName: String,
   val keyPath: Path
) : ConnectorConfiguration {
   override val driverName: String
      get() = GcpConnection.DRIVER_NAME

   override val type: ConnectorType
      get() = ConnectorType.GCP

}
