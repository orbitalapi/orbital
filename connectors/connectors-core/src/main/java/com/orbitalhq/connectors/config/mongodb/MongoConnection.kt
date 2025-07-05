package com.orbitalhq.connectors.config.mongodb

import com.orbitalhq.connectors.ConnectionDriverOptions
import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.IConnectionParameter
import com.orbitalhq.connectors.SimpleDataType
import com.orbitalhq.connectors.connectionParams
import com.orbitalhq.connectors.registry.ConnectorCategory

object MongoConnection {
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      CONNECTION_STRING(ConnectionDriverParam("Mongo instance address", SimpleDataType.STRING, templateParamName = "connectionString")),
      DB_NAME(ConnectionDriverParam("Mongo database name", SimpleDataType.STRING, templateParamName = "dbName")),
   }

   const val DRIVER_NAME = "MongoDb"

   val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
   val driverOptions = ConnectionDriverOptions(
      DRIVER_NAME, "MongoDb", ConnectorCategory.NO_SQL, parameters
   )
}
