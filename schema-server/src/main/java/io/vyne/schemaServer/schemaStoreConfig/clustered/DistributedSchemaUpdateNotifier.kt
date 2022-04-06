package io.vyne.schemaServer.schemaStoreConfig.clustered

import com.hazelcast.topic.ITopic
import com.hazelcast.topic.Message
import com.hazelcast.topic.MessageListener
import io.vyne.schema.api.SchemaSet
import io.vyne.schemaServer.schemaStoreConfig.SchemaUpdateNotifier
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import java.util.UUID

private val logger = KotlinLogging.logger {  }
class DistributedSchemaUpdateNotifier(
   private val topic: ITopic<String>,
   private val validatingStore: ValidatingSchemaStoreClient): SchemaUpdateNotifier, MessageListener<String> {
   private val notifierId = UUID.randomUUID().toString()
   private val schemaSetSink = Sinks.many().replay().latest<SchemaSet>()
   override val schemaSetFlux: Flux<SchemaSet> = schemaSetSink.asFlux()

   init {
       topic.addMessageListener(this)
   }
   override fun sendSchemaUpdate() {
      val schemaSet = validatingStore.schemaSet()
      schemaSetSink.emitNext(schemaSet, emitFailureHandler)
      logger.info { "Sending schema update message from notifier $notifierId" }
      topic.publish(notifierId)
   }

   override fun onMessage(message: Message<String>) {
      logger.info { "Received Schema updated message from cluster member ${message.messageObject}" }
      if (message.messageObject != notifierId) {
         schemaSetSink.emitNext(validatingStore.schemaSet(), emitFailureHandler)
      } else {
         logger.info { "Ignoring self published notifier message." }
      }
   }

   private val emitFailureHandler = Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
      (emitResult
         == Sinks.EmitResult.FAIL_NON_SERIALIZED)
   }
}
