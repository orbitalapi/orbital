package com.orbitalhq.connectors.config.azure

import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorCategory
import com.orbitalhq.utils.obfuscateKeys
import kotlinx.serialization.Serializable

@Serializable
data class ServiceBusConnectionConfiguration(
    override val connectionName: String,
    val connectionParameters: Map<ConnectionParameterName, String>) :
    ConnectorConfiguration {
    override val type: ConnectorCategory = ConnectorCategory.AZURE_SERVICE_BUS
    override fun getUiDisplayProperties(): Map<String, Any> {
        return connectionParameters.obfuscateKeys(
            listOf(
                ServiceBusConnection.Parameters.SharedAccessKey.templateParamName,
                ServiceBusConnection.Parameters.SharedAccessKeyName.templateParamName)
        ) { _, value ->
            obfuscatePassword(value)
        }
    }

    override val driverName: String = ServiceBusConnection.DRIVER_NAME

    constructor(
        connectionName: String,
        sharedAccessKeyName: String,
        sharedAccessKey: String,
        dbName: String,
        connectionParameters: Map<ConnectionParameterName, String> = emptyMap()
    ) : this(
        connectionName,
        connectionParameters +
                mapOf(
                    ServiceBusConnection.Parameters.ENDPOINT.templateParamName to dbName,
                    ServiceBusConnection.Parameters.SharedAccessKeyName.templateParamName to sharedAccessKeyName,
                    ServiceBusConnection.Parameters.SharedAccessKey.templateParamName to sharedAccessKey,
                )
    )

    // "Endpoint=sb://EndPoint;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"
    // So, we need replace a subsection of the string
    fun obfuscatePassword(connectionString: String): String {
        val passwordPattern = "(?<=:)([^/@]+)(?=@)".toRegex()
        return connectionString.replace(passwordPattern, "******")
    }

    val endPointUrl: String
        get() = connectionParameters[ServiceBusConnection.Parameters.ENDPOINT.templateParamName]!!

    val sharedAccessKeyName: String
        get() = connectionParameters[ServiceBusConnection.Parameters.SharedAccessKeyName.templateParamName]!!

    val sharedAccessKey: String
        get() = connectionParameters[ServiceBusConnection.Parameters.SharedAccessKey.templateParamName]!!

    val optionalParameters = connectionParameters
        .minus(ServiceBusConnection.Parameters.ENDPOINT.templateParamName)
        .minus(ServiceBusConnection.Parameters.SharedAccessKeyName.templateParamName)
        .minus(ServiceBusConnection.Parameters.SharedAccessKey.templateParamName)

    val connectionString: String
        get() {
            val optionalParametersString =
                optionalParameters.map { entry -> "${entry.key}=${entry.value}" }.joinToString(";")
            return "Endpoint=sb://$endPointUrl;SharedAccessKeyName=$sharedAccessKeyName;SharedAccessKey=$sharedAccessKey;$optionalParametersString"
        }
}
