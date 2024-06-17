package com.orbitalhq.connectors.aws.sqs

import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListSet

class SqsReceiver(private val sqsClient: SqsAsyncClient, private val receiverOptions: SqsReceiverOptions) {

   private val messagesToBeDeleted: ConcurrentSkipListSet<String> = ConcurrentSkipListSet()
   private val messageDeleteSink = Sinks.many()
      .unicast()
      .onBackpressureBuffer<Message>()

   private val messageDeleteSubscription: Disposable =  messageDeleteSink.asFlux()
      .bufferTimeout(10, Duration.ofSeconds(5))
      .map { messages ->
         val messageDeleteBatchEntry =
            messages.map { DeleteMessageBatchRequestEntry.builder()
               .receiptHandle(it.receiptHandle())
               .id(UUID.randomUUID().toString())
               .build()
            }
         DeleteMessageBatchRequest.builder()
            .queueUrl(receiverOptions.queueName)
            .entries(messageDeleteBatchEntry)
            .build()to messages.map { it.messageId() }.toSet()
      }
      .flatMap { deleteMessageBatchRequest ->
         logger.debug { "Sending SQS Message Deletion batch for ${deleteMessageBatchRequest.first.entries().size} messages" }
         Mono.fromFuture(sqsClient.deleteMessageBatch(deleteMessageBatchRequest.first))
            .onErrorResume { e ->
               logger.error(e) { "Failed to delete SQS message batch from queue ${deleteMessageBatchRequest.first.queueUrl()} containing ${deleteMessageBatchRequest.first.entries().size} messages.  ${e.message}" }
               Mono.empty()
            }.doOnNext {
               logger.info { "deleted batch" }
            }
            .doFinally {
               messagesToBeDeleted.removeAll(deleteMessageBatchRequest.second)
            }
      }
      .subscribe()


   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun receive(): Flux<Message> {
      val sqsRequest = ReceiveMessageRequest.builder()
         .queueUrl(receiverOptions.queueName)
         .maxNumberOfMessages(receiverOptions.maxNumberOfMessagesToFetch)
         .waitTimeSeconds(receiverOptions.pollTimeout.toSeconds().toInt())
         .visibilityTimeout(receiverOptions.visibilityTimeOut)
         .build()


      return  Mono.defer {
         Mono.fromFuture(sqsClient.receiveMessage(sqsRequest))
      }
         .repeat()
         .doOnError { t: Throwable -> logger.error(t) { "Error in calling receive message in sqs queue ${receiverOptions.queueName}"  } }
         .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2)).transientErrors(true))
         .doOnSubscribe {
            logger.info { "Subscribing to SQS queue ${receiverOptions.queueName} on connection ${receiverOptions.awsConnectionConfiguration.connectionName}" }
         }
         .subscribeOn(Schedulers.boundedElastic())
         .flatMapIterable { f ->
            f.messages()
         }
         .filter {
            !messagesToBeDeleted.contains(it.messageId())
         }.map {
            messagesToBeDeleted.add(it.messageId())
            it
         }
         .doOnNext { message ->
            logger.info { "SQS queue ${receiverOptions.queueName} on connection ${receiverOptions.awsConnectionConfiguration.connectionName} received message ${message.messageId()}" }
            messageDeleteSink.emitNext(message, Sinks.EmitFailureHandler.FAIL_FAST)
         }
         .doFinally {
            messageDeleteSubscription.dispose()
         }
   }
}
