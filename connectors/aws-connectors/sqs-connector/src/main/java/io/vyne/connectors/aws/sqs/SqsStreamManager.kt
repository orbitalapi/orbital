package io.vyne.connectors.aws.sqs

import com.google.common.cache.CacheBuilder
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemas.QualifiedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

data class SnsConsumerRequest(
   val connectionName: String,
   val topicName: String,
   val messageType: QualifiedName
)

class SqsStreamManager(private val connectionRegistry: AwsConnectionRegistry,
                       private val schemaProvider: SchemaProvider,
                       private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
   private val cache = CacheBuilder.newBuilder()
      .build<SnsConsumerRequest, SharedFlow<TypedInstance>>()

   private val messageCounter = mutableMapOf<SnsConsumerRequest, AtomicInteger>()

   fun getActiveRequests():List<SnsConsumerRequest> = cache.asMap().keys.toList()

   fun getStream(request: SnsConsumerRequest): Flow<TypedInstance> {
      return cache.get(request) {
         messageCounter[request] = AtomicInteger(0)
         buildSharedFlow(request)
      }
   }

   private fun evictConnection(consumerRequest: SnsConsumerRequest) {
      cache.invalidate(consumerRequest)
      messageCounter.remove(consumerRequest)
      cache.cleanUp()
      logger.debug { "Evicted connection ${consumerRequest.connectionName} / ${consumerRequest.topicName}" }
   }

   private fun buildSharedFlow(request: SnsConsumerRequest): SharedFlow<TypedInstance> {
      logger.info { "Creating new SQS polling subscription for request $request" }
      val messageType = schemaProvider.schema().type(request.messageType).let { type ->
         require(type.name.name == "Stream") { "Expected to receive a Stream type for consuming from Kafka. Instead found ${type.name.parameterizedName}" }
         type.typeParameters[0]
      }
      val schema = schemaProvider.schema()
      val snsReceiverOptions = SnsReceiverOptions(Duration.ofSeconds(1), request.topicName, connectionRegistry.getConnection(request.connectionName))
      return SqsReceiver(snsReceiverOptions)
         .receive()
         .doOnSubscribe {
            logger.info { "Subscriber detected for sqs consumer on ${request.connectionName} / ${request.topicName}" }
         }
         .doOnCancel {
            logger.info { "Subscriber cancel detected for sqs consumer on ${request.connectionName} / ${request.topicName}" }
            evictConnection(request)
         }.map { message ->
            messageCounter[request]?.incrementAndGet()
               ?: logger.warn { "Attempt to increment message counter for consumer on sqs topic ${request.topicName} failed - the counter was not present" }

            logger.debug { "Received message on queue ${request.topicName} with Id ${message.messageId()} " }
            val messageValue = message.body()
            TypedInstance.from(
               messageType,
               messageValue,
               schema
            )

         } .asFlow()
         // SharingStarted.WhileSubscribed() means that we unsubscribe when all subscribers have gone away.
         .shareIn(scope, SharingStarted.WhileSubscribed())
   }
}
