package io.vyne.connectors.aws.s3

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.SimpleDataType
import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConnectorConfiguration
import io.vyne.connectors.connectionParams
import io.vyne.connectors.registry.ConnectorType

object AwsS3Connection {
   const val DRIVER_NAME = "AWS_S3"
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      BUCKET_NAME(ConnectionDriverParam("S3 Bucket Name", SimpleDataType.STRING, templateParamName = "s3BucketName"))
   }

   val parameters: List<ConnectionDriverParam> = AwsConnection.Parameters.values().connectionParams() + Parameters.values().connectionParams()
   val driverOptions = ConnectionDriverOptions(
      DRIVER_NAME, "AWS S3", ConnectorType.AWS_S3, parameters
   )
}

data class AwsS3ConnectionConnectorConfiguration(
   override val connectionName: String,
   override val connectionParameters: Map<ConnectionParameterName, String>
) : AwsConnectionConnectorConfiguration(connectionName, connectionParameters) {
   override val driverName: String
      get() = AwsS3Connection.DRIVER_NAME

   override val type: ConnectorType
      get() = ConnectorType.AWS_S3

   constructor(
      connectionName: String,
      accessKey: String,
      secretKey: String,
      region: String,
      bucket: String,
      endPointOverride: String? = null
   ) : this(
      connectionName,
      endPointOverride?.let {
         mapOf(
            AwsConnection.Parameters.ACCESS_KEY.templateParamName to accessKey,
            AwsConnection.Parameters.SECRET_KEY.templateParamName to secretKey,
            AwsConnection.Parameters.AWS_REGION.templateParamName to region,
            AwsS3Connection.Parameters.BUCKET_NAME.templateParamName to bucket,
            AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName to endPointOverride
         )
      } ?:  mapOf(
         AwsConnection.Parameters.ACCESS_KEY.templateParamName to accessKey,
         AwsConnection.Parameters.SECRET_KEY.templateParamName to secretKey,
         AwsConnection.Parameters.AWS_REGION.templateParamName to region,
         AwsS3Connection.Parameters.BUCKET_NAME.templateParamName to bucket
      )

   )

   companion object {
      fun fromAwsConnectionConfiguration(awsConnectionConnectorConfiguration: AwsConnectionConnectorConfiguration, bucketName: String): AwsS3ConnectionConnectorConfiguration {
        val connectionParams = mutableMapOf<ConnectionParameterName, String>()
         connectionParams.putAll(awsConnectionConnectorConfiguration.connectionParameters)
         connectionParams[AwsS3Connection.Parameters.BUCKET_NAME.templateParamName] = bucketName
         return AwsS3ConnectionConnectorConfiguration(awsConnectionConnectorConfiguration.connectionName, connectionParams.toMap())
      }
   }

}

// Using extension functions to avoid serialization issues with HOCON
val AwsS3ConnectionConnectorConfiguration.bucket: String
   get() {
      return this.connectionParameters[AwsS3Connection.Parameters.BUCKET_NAME.templateParamName] as String
   }
