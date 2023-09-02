package com.orbitalhq.query.runtime.core.dispatcher.aws.sqs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.auth.schemes.AuthSchemeRepository
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryFailedException
import com.orbitalhq.query.QueryResponseMessage
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.query.runtime.QueryMessageCborWrapper
import com.orbitalhq.query.runtime.core.dispatcher.StreamingQueryDispatcher
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.utils.Ids
import reactor.core.publisher.Mono
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toMono
import software.amazon.awssdk.services.sqs.*
import software.amazon.awssdk.services.sqs.model.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Query dispatcher that sends a query for execution on a AWS SQS queue.
 * A temporary queue is created to consumer the results, and
 */
class AwsSqsQueryDispatcher(
    private val servicesRepository: ServicesConfigRepository,
    private val authTokenRepository: AuthSchemeRepository,
    private val connectionsConfigProvider: SourceLoaderConnectorsRegistry,
    private val schemaProvider: SchemaProvider,
    private val sqsClient: SqsAsyncClient,
    private val objectMapper: ObjectMapper = Jackson.newObjectMapperWithDefaults(),
    private val queryQueueAddress: String
) : StreamingQueryDispatcher {
   companion object {
      private val logger = KotlinLogging.logger {}
      const val QUERY_QUEUE_NAME = "query-requests"
   }

   override fun dispatchQuery(
      query: String,
      clientQueryId: String,
      mediaType: String,
      resultMode: ResultMode,
      arguments: Map<String, Any?>
   ): Flux<Any> {

      return createTemporaryQueue(clientQueryId)
         .map { tempQueue ->
            QueryMessage(
               query = query,
               sourcePackages = schemaProvider.schema.packages,
               connections = connectionsConfigProvider.load(),
               authTokens = authTokenRepository.getAllTokens(),
               services = servicesRepository.load(),
               resultMode, mediaType, clientQueryId,
               arguments,
               replyTo = tempQueue
            )
         }
         .flatMap { sendMessageToQueue(it) }
         .flatMapMany { (queryMessage, sendMessageResponse) -> consumeResponses(queryMessage) }
   }

   private fun consumeResponses(queryMessage: QueryMessage): Flux<Any> {

      val request = ReceiveMessageRequest.builder()
         .queueUrl(queryMessage.replyTo!!)
         .maxNumberOfMessages(10)
         .waitTimeSeconds(20)
         .build()
      val queryCompleted = AtomicBoolean(false)

      return Mono.just("start")
         .flatMap {
            logger.debug { "requesting new message on queue ${queryMessage.replyTo}" }
            sqsClient.receiveMessage(request).toMono()
         }
         .flatMapMany { response ->
            logger.debug { "Query ${queryMessage.clientQueryId} received a new batch of ${response.messages().size} response messages" }
            val responsePayloads = response.messages()
               .mapNotNull { message ->
                  val responseJson = message.body()
                  val responsePayload = objectMapper.readValue<QueryResponseMessage>(responseJson)
                  queryCompleted.set(responsePayload.messageKind.isFinalMessage)
                  when (responsePayload.messageKind) {
                     QueryResponseMessage.QueryResponseMessageKind.ERROR -> {
                        logger.info { "Query ${queryMessage.clientQueryId} failed with error ${responsePayload.message}" }
                        throw QueryFailedException(responsePayload.message!!)
                     }

                     QueryResponseMessage.QueryResponseMessageKind.RESULT -> {
                        logger.info { "Query ${queryMessage.clientQueryId} received result" }
                        responsePayload.payload!!
                     }

                     QueryResponseMessage.QueryResponseMessageKind.STREAM_MESSAGE -> {
                        logger.info { "Query ${queryMessage.clientQueryId} received result stream update - AWS Message Id ${message.messageId()}" }
                        responsePayload.payload!!
                     }

                     QueryResponseMessage.QueryResponseMessageKind.END_OF_STREAM -> {
                        logger.info { "Query ${queryMessage.clientQueryId} has finished" }
                        null
                     }
                  }
               }

            deleteConsumedMessages(response, queryMessage.replyTo!!)
               .flatMapMany { Flux.fromIterable(responsePayloads) }
         }
         .repeat { !queryCompleted.get() }
         .doFinally {
            logger.info { "query ${queryMessage.clientQueryId} has completed, cleaning up reply queue" }
            deleteQueue(queryMessage.replyTo!!)
         }
   }

   /**
    * Deletes the messages.  Returns a Mono for chaining, but the boolean is meaningless
    */
   private fun deleteConsumedMessages(response: ReceiveMessageResponse, queueUrl: String): Mono<Boolean> {
      return if (response.hasMessages()) {
         logger.debug { "Deleting messages from $queueUrl" }
         sqsClient.deleteMessageBatch(
            DeleteMessageBatchRequest.builder()
               .queueUrl(queueUrl)
               .entries(
                  response.messages().map {
                     DeleteMessageBatchRequestEntry.builder()
                        .id(it.messageId())
                        .receiptHandle(it.receiptHandle())
                        .build()
                  }
               )
               .build()
         ).toMono()
            .map { deleteBatchResponse ->
               logger.debug { "Deleted ${deleteBatchResponse.successful().size} messages from $queueUrl" }
               deleteBatchResponse.failed().forEach { errorEntry ->
                  logger.warn { "Failed to delete message ${errorEntry.id()} from the response queue, which may lead to it being republished: Code: ${errorEntry.code()} Error: ${errorEntry.message()}" }
               }
               true
            }

      } else {
         // Nothing to delete, so just return the flag
         Mono.just(true)
      }
   }

   private fun sendMessageToQueue(queryMessage: QueryMessage): Mono<Pair<QueryMessage, SendMessageResponse>> {
      val message = QueryMessageCborWrapper.from(queryMessage)
      val jsonMessage = objectMapper.writeValueAsString(message)

      val messageRequest = SendMessageRequest.builder()
         .queueUrl(queryQueueAddress)
         .messageBody(jsonMessage)
         .build()
      return sqsClient.sendMessage(messageRequest)
         .toMono()
         .map { sendMessageResponse -> queryMessage to sendMessageResponse }
   }

   private fun createTemporaryQueue(queryId: String): Mono<String> {
      val queueName = Ids.id("query-response-")
      logger.info { "Creating reply queue for query $queryId named $queueName" }
      val createQueueRequest = CreateQueueRequest.builder()
         .queueName(queueName)
//         .attributes(mapOf(QueueAttributeName.FIFO_QUEUE to "true"))
         .build()
      return sqsClient.createQueue(createQueueRequest)
         .toMono()
         .map { message ->
            "Query $queryId will listen for responses on queue ${message.queueUrl()}"
            message.queueUrl()
         }
   }

   private fun deleteQueue(queueUrl: String) {
      val deleteQueueRequest = DeleteQueueRequest.builder()
         .queueUrl(queueUrl)
         .build()
      sqsClient.deleteQueue(deleteQueueRequest).get()
   }
}

