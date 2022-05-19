package io.vyne.connectors.aws.core

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.SimpleDataType
import io.vyne.connectors.connectionParams
import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType
import java.io.Serializable

object AwsConnection {
   const val DRIVER_NAME = "AWS"

   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      ACCESS_KEY(ConnectionDriverParam("Aws access key", SimpleDataType.STRING, templateParamName = "awsAccessKey")),
      SECRET_KEY(ConnectionDriverParam("Aws secret key", SimpleDataType.STRING, templateParamName = "awsSecretKey")),
      AWS_REGION(
         ConnectionDriverParam(
            "Aws region",
            SimpleDataType.STRING,
            defaultValue = "eu-west-2",
            templateParamName = "awsRegion",
         )
      ),
      ENDPOINT_OVERRIDE(
         ConnectionDriverParam(
            "Aws end point override",
            SimpleDataType.STRING,
            templateParamName = "endPointOverride",
            defaultValue = null,
            required = false, visible = false
         )
      )
   }

   val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
   val driverOptions = ConnectionDriverOptions(
      DRIVER_NAME, "Aws", ConnectorType.AWS, parameters
   )
}

abstract class AwsConnectionConnectorConfiguration(
   override val connectionName: String,
   open val connectionParameters: Map<ConnectionParameterName, String>
) : ConnectorConfiguration, Serializable {
   override val driverName: String = AwsConnection.DRIVER_NAME
}

data class AwsConnectionConfiguration(
   override val connectionName: String,
   override val connectionParameters: Map<ConnectionParameterName, String>,
   override val type: ConnectorType = ConnectorType.AWS
) : AwsConnectionConnectorConfiguration(connectionName, connectionParameters), Serializable {
   constructor(
      connectionName: String,
      accessKey: String,
      secretKey: String,
      region: String,
      endPointOverride: String? = null
   ) : this(
      connectionName,
      endPointOverride?.let {
         mapOf(
            AwsConnection.Parameters.ACCESS_KEY.templateParamName to accessKey,
            AwsConnection.Parameters.SECRET_KEY.templateParamName to secretKey,
            AwsConnection.Parameters.AWS_REGION.templateParamName to region,
            AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName to endPointOverride
         )
      } ?: mapOf(
         AwsConnection.Parameters.ACCESS_KEY.templateParamName to accessKey,
         AwsConnection.Parameters.SECRET_KEY.templateParamName to secretKey,
         AwsConnection.Parameters.AWS_REGION.templateParamName to region
      )
   )
}


// Using extension functions to avoid serialization issues with HOCON
val AwsConnectionConnectorConfiguration.accessKey: String
   get() {
      return this.connectionParameters[AwsConnection.Parameters.ACCESS_KEY.templateParamName] as String
   }

val AwsConnectionConnectorConfiguration.secretKey: String
   get() {
      return this.connectionParameters[AwsConnection.Parameters.SECRET_KEY.templateParamName] as String
   }

val AwsConnectionConnectorConfiguration.region: String
   get() {
      return this.connectionParameters[AwsConnection.Parameters.AWS_REGION.templateParamName] as String
   }

val AwsConnectionConnectorConfiguration.endPointOverride: String?
   get() {
      return this.connectionParameters[AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName]
   }
