package com.orbitalhq.connectors.aws.sqs

import com.orbitalhq.connectors.aws.configureWithExplicitValuesIfProvided
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

class SqsReceiver(private val sqsClient: SqsAsyncClient, private val receiverOptions: SqsReceiverOptions) {

   private val messageDeleteSink = Sinks.many()
      .unicast()
      .onBackpressureBuffer<Message>()

   private val messageDeleteSubscription: Disposable =  messageDeleteSink.asFlux()
      .bufferTimeout(10, Duration.ofSeconds(5))
      .map { messages ->
         val messageDeleteBatchEntry =
            messages.map { DeleteMessageBatchRequestEntry.builder().receiptHandle(it.receiptHandle()).build() }
         DeleteMessageBatchRequest.builder()
            .queueUrl(receiverOptions.queueName)
            .entries(messageDeleteBatchEntry)
            .build()
      }
      .flatMap { deleteMessageBatchRequest ->
         logger.debug { "Sending SQS Message Deletion batch for ${deleteMessageBatchRequest.entries().size} messages" }
         Mono.fromFuture(sqsClient.deleteMessageBatch(deleteMessageBatchRequest))
            .onErrorResume { e ->
               logger.error(e) { "Failed to delete SQS message batch from queue ${deleteMessageBatchRequest.queueUrl()} containing ${deleteMessageBatchRequest.entries().size} messages.  ${e.message}" }
               Mono.empty()
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
         .build()
      return Mono.fromFuture(sqsClient.receiveMessage(sqsRequest))
         .repeat()
         .doOnSubscribe {
            logger.info { "Subscribing to SQS queue ${receiverOptions.queueName} on connection ${receiverOptions.awsConnectionConfiguration.connectionName}" }
         }
         .retry()
         .subscribeOn(Schedulers.boundedElastic())
         .flatMapIterable { f -> f.messages() }
         .doOnNext { message ->
            logger.debug { "SQS queue ${receiverOptions.queueName} on connection ${receiverOptions.awsConnectionConfiguration.connectionName} received message ${message.messageId()}" }
            messageDeleteSink.emitNext(message, Sinks.EmitFailureHandler.FAIL_FAST)
         }
         .doFinally {
            messageDeleteSubscription.dispose()
         }
   }



//   fun receive(): Flux<Message> {
//      return withHandler { scheduler: Scheduler, handler: SqsConsumerHandler ->
//         handler
//            .receive()
//            .filter { it.hasMessages() }
//            .flatMapIterable { it.messages() }
//            .publishOn(scheduler)
//      }
//   }
//
//   private fun <T> withHandler(function: BiFunction<Scheduler,SqsConsumerHandler, Flux<T>>): Flux<T> {
//
//      return Flux.usingWhen(
//         Mono.fromCallable {
//            SqsConsumerHandler(sqsConnection)
//         },
//         { handler: SqsConsumerHandler ->
//            Flux.using(
//               { Schedulers.single(Supplier { Schedulers.immediate() }.get()) },
//               { scheduler: Scheduler -> function.apply(scheduler, handler) }) { obj: Scheduler -> obj.dispose() }
//         }
//      ) { handler: SqsConsumerHandler -> handler.close() }
//   }


}
