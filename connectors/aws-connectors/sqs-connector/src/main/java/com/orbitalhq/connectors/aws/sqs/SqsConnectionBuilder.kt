package com.orbitalhq.connectors.aws.sqs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.connectors.aws.configureWithExplicitValuesIfProvided
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnection
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.models.format.FormatRegistry
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.time.Duration

class SqsConnectionBuilder(
   private val connectionRegistry: AwsConnectionRegistry,
   private val formatRegistry: FormatRegistry,
   private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
   fun buildPublisher(connectionName: String, queueName: String): SqsPublisher {
      val client = buildClient(connectionName)
      return SqsPublisher(client, queueName, connectionName, formatRegistry, objectMapper)
   }

   fun buildReceiver(
      connectionName: String,
      topicName: String
   ): SqsReceiver {
      val awsConnectionConfig = connectionRegistry.getConnection(connectionName)
      val pollTimeout = awsConnectionConfig.connectionParameters?.get(AwsConnection.Parameters.SQS_RECEIVE_REQUEST_WAIT_TIME.templateParamName)?.let {
         Duration.ofSeconds(it.toLong())
      } ?: Duration.ofSeconds(1)

      val maxMessagesToReturn = awsConnectionConfig.connectionParameters?.get(AwsConnection.Parameters.SQS_RECEIVE_MAX_NUMBER_OF_MESSAGES.templateParamName)
         ?.toInt() ?: AwsConnection.Parameters.SQS_RECEIVE_MAX_NUMBER_OF_MESSAGES.param.defaultValue!! as Int


      val visibilityTimeout = awsConnectionConfig.connectionParameters?.get(AwsConnection.Parameters.SQS_RECEIVE_VISIBILITY_TIMEOUT.templateParamName)
         ?.toInt() ?: AwsConnection.Parameters.SQS_RECEIVE_VISIBILITY_TIMEOUT.param.defaultValue!! as Int

      val sqsReceiverOptions = SqsReceiverOptions(
         pollTimeout,
         topicName,
         awsConnectionConfig,
         maxMessagesToReturn,
         visibilityTimeout
      )

      val client = buildClient(connectionName)

      return SqsReceiver(client, sqsReceiverOptions)
   }

   private fun buildClient(awsConnectionConfig: AwsConnectionConfiguration): SqsAsyncClient {
      return SqsAsyncClient.builder()
         .configureWithExplicitValuesIfProvided(awsConnectionConfig)
         .build()
   }

   private fun buildClient(connectionName: String): SqsAsyncClient {
      val awsConnectionConfig = connectionRegistry.getConnection(connectionName)
      return buildClient(awsConnectionConfig)
   }

}

data class SqsReceiverOptions(
   val pollTimeout: Duration,
   val queueName: String,
   val awsConnectionConfiguration: AwsConnectionConfiguration,
   val maxNumberOfMessagesToFetch: Int,
   val visibilityTimeOut: Int
)
