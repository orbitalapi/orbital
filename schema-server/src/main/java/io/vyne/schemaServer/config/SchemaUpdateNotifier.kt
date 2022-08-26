package io.vyne.schemaServer.config

import io.vyne.schema.api.SchemaSet
import io.vyne.schema.publisher.PackagesUpdatedMessage
import io.vyne.schema.publisher.SchemaUpdatedMessage
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import io.vyne.schemas.Schema
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks

interface SchemaUpdateNotifier {
   fun emitCurrentSchemaSet()
   val schemaSetFlux: Flux<SchemaSet>
   val schemaUpdates: Flux<SchemaUpdatedMessage>

   fun sendSchemaUpdated(message: SchemaUpdatedMessage)

   // TODO : This is in the wrong place.
   // Need to move into the ValidatingSchemaStore, which should emit the
   // SchemaUpdatedMessage as part of the change submission process.
   fun buildAndSendSchemaUpdated(message: PackagesUpdatedMessage, oldSchema: Schema)

}

class LocalSchemaNotifier(private val validatingStore: ValidatingSchemaStoreClient) : SchemaUpdateNotifier {
   private val schemaSetSink = Sinks.many().replay().latest<SchemaSet>()
   override val schemaSetFlux: Flux<SchemaSet> = schemaSetSink.asFlux()

   private val schemaUpdatesSink = Sinks.many().replay().latest<SchemaUpdatedMessage>()
   override val schemaUpdates: Flux<SchemaUpdatedMessage> = schemaUpdatesSink.asFlux()

   private val emitFailureHandler = Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
      (emitResult
         == Sinks.EmitResult.FAIL_NON_SERIALIZED)
   }

   override fun emitCurrentSchemaSet() {
      schemaSetSink.emitNext(validatingStore.schemaSet, emitFailureHandler)
   }

   override fun sendSchemaUpdated(message: SchemaUpdatedMessage) {
      schemaUpdatesSink.emitNext(message, emitFailureHandler)
   }

   override fun buildAndSendSchemaUpdated(message: PackagesUpdatedMessage, oldSchema: Schema) {
      sendSchemaUpdated(
         SchemaUpdatedMessage(
            packageUpdates = message,
            schema = validatingStore.schemaSet.schema,
            oldSchema = oldSchema,
            errors = validatingStore.lastCompilationMessages
         )
      )
   }


}
