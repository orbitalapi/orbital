package com.orbitalhq.connectors.config.mongodb

import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import com.orbitalhq.utils.obfuscateKeys
import kotlinx.serialization.Serializable

@Serializable
class MongoConnectionConfiguration( override val connectionName: String,
                                    val connectionParameters: Map<ConnectionParameterName, String>):
   ConnectorConfiguration {
   override val type: ConnectorType = ConnectorType.NO_SQL
   override fun getUiDisplayProperties(): Map<String, Any> {
      return emptyMap()
   }

   override val driverName: String = MongoConnection.DRIVER_NAME
   }
