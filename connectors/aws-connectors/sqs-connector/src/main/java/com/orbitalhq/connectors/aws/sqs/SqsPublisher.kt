package com.orbitalhq.connectors.aws.sqs

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.schemas.Schema
import mu.KotlinLogging
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

class SqsPublisher(
   private val sqsClient: SqsAsyncClient,
   private val queueName: String,
   private val connectionName: String,
   private val formatRegistry: FormatRegistry,
   private val objectMapper: ObjectMapper
) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun sendMessage(instance: TypedInstance, schema: Schema): Mono<TypedInstance> {

      val modelFormat = formatRegistry.forType(instance.type)
      val message = if (modelFormat != null) {
         val serializedMessage = modelFormat.serializer.write(instance,schema)
         if (serializedMessage == null) {
            if (instance !is TypedNull) {
               logger.warn { "Model format ${modelFormat::class.simpleName} returned null when serializing instance, but instance was not null. Instance = ${writeAsJson(instance)}" }
            }
            return Mono.just(TypedNull.create(instance.type))
         }
         require(serializedMessage is String) { "SQS only supports sending String messages, however format ${modelFormat::class.simpleName} produced a message of type ${serializedMessage::class.simpleName}"}
         serializedMessage
      } else {
         writeAsJson(instance)
      }
      return sendMessage(message)
         .map { instance }

   }

   private fun writeAsJson(instance: TypedInstance): String {
      return objectMapper
         .writerWithDefaultPrettyPrinter()
         .writeValueAsString(instance.toRawObject())
   }

   fun sendMessage(messageBody: String): Mono<SendMessageResponse> {
      val request = SendMessageRequest.builder()
         .messageBody(messageBody)
         .queueUrl(queueName)
         .build()
      return Mono.fromFuture {
         sqsClient.sendMessage(request)
      }.doOnSubscribe {
         logger.debug { "Sending message to SQS Queue $queueName on connection $connectionName" }
      }
   }
}
