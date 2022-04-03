package io.vyne.connectors.aws.sqs

import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.endPointOverride
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.secretKey
import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.SqsClientBuilder
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import java.net.URI
import java.time.Duration

private val logger = KotlinLogging.logger {  }
class SqsConnection(private val receiverOptions: SnsReceiverOptions) {
   fun poll(): ReceiveMessageResponse {
      val builder = createSnsClientBuilder()
      val snsRequest = ReceiveMessageRequest.builder()
         .queueUrl(receiverOptions.queueName)
         .maxNumberOfMessages(receiverOptions.maxNumberOfMessagesToFetch)
         .waitTimeSeconds(receiverOptions.pollTimeout.toSeconds().toInt())
         .build()

      val client = builder.build()
      return try {
         return client.receiveMessage(snsRequest)
      } catch (e: Exception) {
         logger.error(e) { "Error in fetching messages from Sqs"  }
         ReceiveMessageResponse.builder().build()
      } finally {
          client.close()
      }
   }

   fun deleteProcessedMessages(receiptIds: List<String>) {
      val builder = createSnsClientBuilder()
      val client = builder.build()
      val entries =  receiptIds.map { DeleteMessageBatchRequestEntry.builder().receiptHandle(it).build() }
      val deleteBatchRequest = DeleteMessageBatchRequest.builder().queueUrl(receiverOptions.queueName).entries(entries).build()
      try {
          client.deleteMessageBatch(deleteBatchRequest)
      } catch (e: Exception) {
         logger.error(e) { "Error deleting messages from sqs for request $deleteBatchRequest"  }
      } finally {
         client.close()
      }
   }

   private fun createSnsClientBuilder(): SqsClientBuilder {
      val awsConnectionConfiguration = receiverOptions.awsConnectionConfiguration
      val builder = SqsClient
         .builder()
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
            awsConnectionConfiguration.accessKey,
            awsConnectionConfiguration.secretKey)))
         .region(Region.of(awsConnectionConfiguration.region))


      if (awsConnectionConfiguration.endPointOverride != null) {
         builder.endpointOverride(URI.create(awsConnectionConfiguration.endPointOverride!!))
      }
      return builder
   }
}

data class SnsReceiverOptions(
   val pollTimeout: Duration,
   val queueName: String,
   val awsConnectionConfiguration: AwsConnectionConfiguration,
   // This can be at most 10
   val maxNumberOfMessagesToFetch: Int = 10)
