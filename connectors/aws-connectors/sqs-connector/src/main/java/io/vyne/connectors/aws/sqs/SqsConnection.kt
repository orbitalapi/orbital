package io.vyne.connectors.aws.sqs

import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.configureWithExplicitValuesIfProvided
import mu.KotlinLogging
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.SqsClientBuilder
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import java.time.Duration

private val logger = KotlinLogging.logger {  }
class SqsConnection(private val receiverOptions: SqsReceiverOptions) {
   fun poll(): ReceiveMessageResponse {
      val builder = createSqsClientBuilder()
      val sqsRequest = ReceiveMessageRequest.builder()
         .queueUrl(receiverOptions.queueName)
         .maxNumberOfMessages(receiverOptions.maxNumberOfMessagesToFetch)
         .waitTimeSeconds(receiverOptions.pollTimeout.toSeconds().toInt())
         .build()

      val client = builder.build()
      return try {
         return client.receiveMessage(sqsRequest)
      } catch (e: Exception) {
         logger.error(e) { "Error in fetching messages from Sqs" }
         ReceiveMessageResponse.builder().build()
      } finally {
          client.close()
      }
   }

   fun deleteProcessedMessages(receiptIds: List<String>) {
      val builder = createSqsClientBuilder()
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

   private fun createSqsClientBuilder(): SqsClientBuilder {
      val awsConnectionConfiguration = receiverOptions.awsConnectionConfiguration
      return SqsClient
         .builder()
         .configureWithExplicitValuesIfProvided(awsConnectionConfiguration)
   }
}

data class SqsReceiverOptions(
   val pollTimeout: Duration,
   val queueName: String,
   val awsConnectionConfiguration: AwsConnectionConfiguration,
   // This can be at most 10
   val maxNumberOfMessagesToFetch: Int = 10
)
