package com.orbitalhq.connectors.nosql.mongodb

import com.mongodb.ConnectionString
import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.valueOrThrowNiceMessage

val  MongoConnectionConfiguration.connectionString: ConnectionString
   get() {
      val connectionString =  this.connectionParameters.valueOrThrowNiceMessage(MongoConnection.Parameters.CONNECTION_STRING.templateParamName)
      // Below will throw if the connection string is not a valid MongoConnection String.
     return ConnectionString(connectionString)
   }

val MongoConnectionConfiguration.database: String
   get() {
      return this.connectionString.database ?:  this.connectionParameters.valueOrThrowNiceMessage(MongoConnection.Parameters.DB_NAME.templateParamName)
   }

