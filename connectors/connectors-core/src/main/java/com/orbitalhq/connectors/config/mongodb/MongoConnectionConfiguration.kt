package com.orbitalhq.connectors.config.mongodb

import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import com.orbitalhq.utils.obfuscateKeys
import kotlinx.serialization.Serializable

@Serializable
data class MongoConnectionConfiguration(
   override val connectionName: String,
   val connectionParameters: Map<ConnectionParameterName, String>
) :
   ConnectorConfiguration {
   override val type: ConnectorType = ConnectorType.NO_SQL
   override fun getUiDisplayProperties(): Map<String, Any> {
      return connectionParameters.obfuscateKeys(listOf("connectionString")) { _, value ->
            obfuscatePassword(value)
         }
   }

   override val driverName: String = MongoConnection.DRIVER_NAME

   constructor(
      connectionName: String,
      connectionString: String,
      dbName: String,
      connectionParameters: Map<ConnectionParameterName, String> = emptyMap()
   ) : this(
      connectionName,
         connectionParameters +
         mapOf(
            MongoConnection.Parameters.DB_NAME.templateParamName to dbName,
            MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString,
         )
   )

   // connectionString=mongodb+srv://orbital:PASSWORD@orbital.xxxx.mongodb.net/?retryWrites=true&w=majority&appName=Orbital
   // So, we need replace a subsection of the string
   fun obfuscatePassword(connectionString: String): String {
      val passwordPattern = "(?<=:)([^/@]+)(?=@)".toRegex()
      return connectionString.replace(passwordPattern, "******")
   }
}
