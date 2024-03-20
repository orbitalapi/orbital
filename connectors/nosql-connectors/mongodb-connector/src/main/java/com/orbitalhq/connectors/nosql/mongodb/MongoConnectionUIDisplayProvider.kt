package com.orbitalhq.connectors.nosql.mongodb

import com.mongodb.ConnectionString
import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectionUIDisplayProvider
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.valueOrThrowNiceMessage

class MongoConnectionUIDisplayProvider: ConnectionUIDisplayProvider {
   override fun uiProps(connectorConfig: ConnectorConfiguration): Map<String, Any>? {
     return (connectorConfig as? MongoConnectionConfiguration)?.let { mongoConnectionConfiguration ->
         val connectionString =  mongoConnectionConfiguration.connectionParameters.valueOrThrowNiceMessage(MongoConnection.Parameters.CONNECTION_STRING.templateParamName)
         val mongoConnectionString = ConnectionString(connectionString)

         mapOf("hosts" to mongoConnectionString.hosts,
            "sslEnabled" to (mongoConnectionString.sslEnabled ?: false),
            "database" to connectorConfig.database,
            "username" to (mongoConnectionString.username ?: ""))
      }
   }
}
