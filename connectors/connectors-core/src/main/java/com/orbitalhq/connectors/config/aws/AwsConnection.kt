package com.orbitalhq.connectors.config.aws

import com.orbitalhq.connectors.ConnectionDriverOptions
import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.IConnectionParameter
import com.orbitalhq.connectors.SimpleDataType
import com.orbitalhq.connectors.connectionParams
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorCategory
import java.io.Serializable

object AwsConnection {
   const val DRIVER_NAME = "AWS"

   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      ACCESS_KEY(
         ConnectionDriverParam(
            "AWS access key",
            SimpleDataType.STRING,
            templateParamName = AwsConnectionConfiguration::accessKey.name,
            description = "Leave blank to use your AWS account’s default settings automatically.",
            defaultValue = null,
            required = false,
            isConstructorParameter = true,
            sensitive = true
         )
      ),
      SECRET_KEY(
         ConnectionDriverParam(
            "AWS secret key",
            SimpleDataType.STRING,
            description = "Leave blank to use your AWS account’s default settings automatically.",
            templateParamName = AwsConnectionConfiguration::secretKey.name,
            defaultValue = null,
            required = false,
            isConstructorParameter = true,
            sensitive = true
         )
      ),
      AWS_REGION(
         ConnectionDriverParam(
            "AWS region",
            SimpleDataType.STRING,
            description = "Leave blank to use your AWS account’s default settings automatically.",
            defaultValue = null,
            required = false,
            templateParamName = AwsConnectionConfiguration::region.name,
            isConstructorParameter = true
         )
      ),
      ENDPOINT_OVERRIDE(
         ConnectionDriverParam(
            "AWS endpoint override",
            SimpleDataType.STRING,
            templateParamName = AwsConnectionConfiguration::endPointOverride.name,
            defaultValue = null,
            required = false,
            visible = false,
            isConstructorParameter = true
         )
      ),
      SQS_RECEIVE_REQUEST_WAIT_TIME(
         ConnectionDriverParam(
            "SQS receive message request wait time in seconds",
            SimpleDataType.NUMBER,
            defaultValue = 1,
            templateParamName = "sqsReceiveRequestWaitTime",
            visible = false,
         )
      ),
      SQS_RECEIVE_MAX_NUMBER_OF_MESSAGES(
         ConnectionDriverParam(
            "SQS maximum number of messages to return.",
            SimpleDataType.NUMBER,
            defaultValue = 10,
            templateParamName = "sqsReceiveMaxNumberOfMessages",
            visible = false,
         )
      ),
      SQS_RECEIVE_VISIBILITY_TIMEOUT(
         ConnectionDriverParam(
            "SQS receive duration (in seconds)",
            SimpleDataType.NUMBER,
            defaultValue = 300,
            templateParamName = "sqsReceiveVisibilityTimeout",
            visible = false,
            description = "The duration that the received messages are hidden from subsequent retrieve requests after being retrieved by a ReceiveMessage request."
         )
      ),
   }

   val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
   val driverOptions = ConnectionDriverOptions(
      DRIVER_NAME, "AWS", ConnectorCategory.AWS, parameters
   )
}

//interface AwsConnectionConnectorConfiguration : ConnectorConfiguration, Serializable {
//   override val driverName: String
//   get()  = AwsConnection.DRIVER_NAME
//}


@kotlinx.serialization.Serializable
data class AwsConnectionConfiguration(
   override val connectionName: String,
   val region: String?,
   val accessKey: String?,
   val secretKey: String?,
   val endPointOverride: String? = null,
   val connectionParameters: Map<ConnectionParameterName, String>? = null
) : ConnectorConfiguration, Serializable {
   override val type: ConnectorCategory = ConnectorCategory.AWS
   override fun getUiDisplayProperties(): Map<String, Any> {
      val result = if (region != null) {
         mapOf(
            "region" to region
         )
      } else emptyMap()

      return when {
         endPointOverride != null && connectionParameters != null -> result + mapOf("endpointOverride" to endPointOverride) + connectionParameters
         endPointOverride != null -> result + mapOf("endpointOverride" to endPointOverride)
         connectionParameters != null -> result + connectionParameters
         else -> result
      }
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
