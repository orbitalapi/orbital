package io.vyne.connectors.aws.core

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.SimpleDataType
import io.vyne.connectors.connectionParams
import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region
import java.io.Serializable


fun <T : AwsClientBuilder<*, *>> T.configureWithExplicitValuesIfProvided(configuration: AwsConnectionConnectorConfiguration): T {
   if (configuration.accessKey != null && configuration.secretKey != null) {
      this.credentialsProvider(
         StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
               configuration.accessKey,
               configuration.secretKey
            )
         )
      )
   }

   if (configuration.region != null) {
      this.region(Region.of(configuration.region))
   }

   if (configuration.endPointOverride != null) {
      this.endpointOverride(java.net.URI.create(configuration.endPointOverride))
   }

   return this
}

object AwsConnection {
   const val DRIVER_NAME = "AWS"

   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      ACCESS_KEY(
         ConnectionDriverParam(
            "AWS access key",
            SimpleDataType.STRING,
            templateParamName = "awsAccessKey",
            defaultValue = null,
            required = false,
         )
      ),
      SECRET_KEY(
         ConnectionDriverParam(
            "AWS secret key",
            SimpleDataType.STRING,
            templateParamName = "awsSecretKey",
            defaultValue = null,
            required = false,
         )
      ),
      AWS_REGION(
         ConnectionDriverParam(
            "AWS region",
            SimpleDataType.STRING,
            defaultValue = null,
            required = false,
            templateParamName = "awsRegion",
         )
      ),
      ENDPOINT_OVERRIDE(
         ConnectionDriverParam(
            "AWS endpoint override",
            SimpleDataType.STRING,
            templateParamName = "endPointOverride",
            defaultValue = null,
            required = false,
            visible = false
         )
      )
   }

   val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
   val driverOptions = ConnectionDriverOptions(
      DRIVER_NAME, "AWS", ConnectorType.AWS, parameters
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
      region: String,
      accessKey: String,
      secretKey: String,
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
         AwsConnection.Parameters.AWS_REGION.templateParamName to region
      )
   )
}


// Using extension functions to avoid serialization issues with HOCON
val AwsConnectionConnectorConfiguration.accessKey: String?
   get() {
      return this.connectionParameters[AwsConnection.Parameters.ACCESS_KEY.templateParamName]
   }

val AwsConnectionConnectorConfiguration.secretKey: String?
   get() {
      return this.connectionParameters[AwsConnection.Parameters.SECRET_KEY.templateParamName]
   }

val AwsConnectionConnectorConfiguration.region: String?
   get() {
      return this.connectionParameters[AwsConnection.Parameters.AWS_REGION.templateParamName]
   }

val AwsConnectionConnectorConfiguration.endPointOverride: String?
   get() {
      return this.connectionParameters[AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName]
   }
