package com.orbitalhq.connectors.aws.sqs

import arrow.core.Either
import arrow.core.right
import com.google.common.cache.CacheBuilder
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.QualifiedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class SqsConsumerRequest(
   val connectionName: String,
   val topicName: String,
   val messageType: QualifiedName
)

class SqsStreamManager(private val connectionBuilder: SqsConnectionBuilder,
                       private val schemaProvider: SchemaProvider,
                       private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
   private val cache = CacheBuilder.newBuilder()
      .build<SqsConsumerRequest, SharedFlow<Either<StreamErrorMessage, TypedInstance>>>()

   fun getStream(request: SqsConsumerRequest): Flow<Either<StreamErrorMessage, TypedInstance>> {
      return cache.get(request) {
         buildSharedFlow(request)
      }
   }

   private fun evictConnection(consumerRequest: SqsConsumerRequest) {
      cache.invalidate(consumerRequest)
      cache.cleanUp()
      logger.debug { "Evicted connection ${consumerRequest.connectionName} / ${consumerRequest.topicName}" }
   }

   private fun buildSharedFlow(request: SqsConsumerRequest): SharedFlow<Either<StreamErrorMessage, TypedInstance>> {
      logger.info { "Creating new SQS polling subscription for request $request" }
      val messageType = schemaProvider.schema.type(request.messageType).let { type ->
         require(type.name.name == "Stream") { "Expected to receive a Stream type for consuming from Kafka. Instead found ${type.name.parameterizedName}" }
         type.typeParameters[0]
      }
      val schema = schemaProvider.schema

      val receiver = connectionBuilder.buildReceiver(
         request.connectionName,
         request.topicName,
      )
      return receiver
         .receive()
         .doOnSubscribe {
            logger.info { "Subscriber detected for sqs consumer on ${request.connectionName} / ${request.topicName}" }
         }
         .doOnCancel {
            logger.info { "Subscriber cancel detected for sqs consumer on ${request.connectionName} / ${request.topicName}" }
            evictConnection(request)
         }.map { message ->
            logger.debug { "Received message on queue ${request.topicName} with Id ${message.messageId()} " }
            val messageValue = message.body()
            try {
               Either.Right(TypedInstance.from(
                  messageType,
                  messageValue,
                  schema
               ))
            } catch (e: Exception) {
               Either.Left(StreamErrorMessage.fromException(e, messageType.paramaterizedName))
            }

         }.asFlow()
         .catch {
            logger.error(it) { "Error in Sqs listener"  }
            // see the error handling notes for SharedFlow:
            // https://github.com/Kotlin/kotlinx.coroutines/issues/2034
            this.emit(ErrorType.errorMessage(it.message ?: "error in sqs listener", schemaProvider.schema).right())
         }
         // SharingStarted.WhileSubscribed() means that we unsubscribe when all subscribers have gone away.
         .shareIn(scope, SharingStarted.WhileSubscribed())
   }
}
