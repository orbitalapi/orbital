package com.orbitalhq.connectors.azure.blob.registry

import com.orbitalhq.connectors.ConnectionDriverOptions
import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.IConnectionParameter
import com.orbitalhq.connectors.SimpleDataType
import com.orbitalhq.connectors.connectionParams
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType

data class AzureStorageConnectorConfiguration(
   override val connectionName: String, val connectionParameters: Map<ConnectionParameterName, String>) : ConnectorConfiguration {
   override val type: ConnectorType
      get() = ConnectorType.AZURE_STORAGE

   override val driverName: String
      get() = AzureStorageConnection.DRIVER_NAME

   constructor(
      connectionName: String,
      connectionString: String
   ) : this(
      connectionName,
     mapOf(
        AzureStorageConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString,
      )
   )

   val connectionString: String
      get() {
         return this.connectionParameters[AzureStorageConnection.Parameters.CONNECTION_STRING.templateParamName] as String
      }
}

object AzureStorageConnection {
   const val DRIVER_NAME = "AZURE_STORAGE"
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      CONNECTION_STRING(ConnectionDriverParam("Azure Storage Connection String", SimpleDataType.STRING, templateParamName = "azureStorageConnectionStr"))
   }

   val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
   val driverOptions = ConnectionDriverOptions(
      DRIVER_NAME, "Azure Storage", ConnectorType.AZURE_STORAGE, parameters
   )

}
