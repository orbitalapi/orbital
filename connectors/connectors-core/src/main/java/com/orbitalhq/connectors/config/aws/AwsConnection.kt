package com.orbitalhq.connectors.config.aws

import com.orbitalhq.connectors.*
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import java.io.Serializable

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

//interface AwsConnectionConnectorConfiguration : ConnectorConfiguration, Serializable {
//   override val driverName: String
//   get()  = AwsConnection.DRIVER_NAME
//}


@kotlinx.serialization.Serializable
data class AwsConnectionConfiguration(
   override val connectionName: String,
   val region: String,
   val accessKey: String,
   val secretKey: String,
   val endPointOverride: String? = null
) : ConnectorConfiguration, Serializable {
   override val type: ConnectorType = ConnectorType.AWS
   override fun getUiDisplayProperties(): Map<String, Any> {
      val result = mapOf(
         "region" to region
      )
      return if (endPointOverride != null) {
         result + mapOf("endpointOverride" to endPointOverride)
      } else result
   }

   override val driverName: String = AwsConnection.DRIVER_NAME
}

//
//// Using extension functions to avoid serialization issues with HOCON
//val AwsConnectionConnectorConfiguration.accessKey: String?
//   get() {
//      return this.connectionParameters[AwsConnection.Parameters.ACCESS_KEY.templateParamName]
//   }
//
//val AwsConnectionConnectorConfiguration.secretKey: String?
//   get() {
//      return this.connectionParameters[AwsConnection.Parameters.SECRET_KEY.templateParamName]
//   }
//
//val AwsConnectionConnectorConfiguration.region: String?
//   get() {
//      return this.connectionParameters[AwsConnection.Parameters.AWS_REGION.templateParamName]
//   }
//
//val AwsConnectionConnectorConfiguration.endPointOverride: String?
//   get() {
//      return this.connectionParameters[AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName]
//   }
