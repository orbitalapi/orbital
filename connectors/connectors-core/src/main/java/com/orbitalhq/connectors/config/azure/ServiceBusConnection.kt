package com.orbitalhq.connectors.config.azure

import com.orbitalhq.connectors.ConnectionDriverOptions
import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.IConnectionParameter
import com.orbitalhq.connectors.SimpleDataType
import com.orbitalhq.connectors.connectionParams
import com.orbitalhq.connectors.registry.ConnectorType

object ServiceBusConnection {
    enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
        ENDPOINT(ConnectionDriverParam("The URL that points to the Azure Service Bus instance you want to connect to", SimpleDataType.STRING, templateParamName = "endPoint")),
        SharedAccessKeyName(ConnectionDriverParam("The name of the shared access key that provides security credentials for authentication", SimpleDataType.STRING, templateParamName = "sharedAccessKeyName")),
        SharedAccessKey(ConnectionDriverParam("The actual security key associated with the shared access key name used to establish secure communication.", SimpleDataType.STRING, templateParamName = "sharedAccessKey"))
    }

    const val DRIVER_NAME = "AzureServiceBus"

    val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
    val driverOptions = ConnectionDriverOptions(
        DRIVER_NAME, "Azure Service Bus", ConnectorType.AZURE_SERVICE_BUS, parameters
    )
}