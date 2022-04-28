package io.vyne.schema.consumer

import io.vyne.schema.api.SchemaSet
import io.vyne.schemas.SchemaSetChangedEvent
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks


/**
 * A emitting failure handler that continues if the failure reason
 * relates to threading (non serialized)
 */
val ContinueIfNonSerialHandler = Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
   (emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED)
}

abstract class SchemaSetChangedEventRepository : SchemaChangedEventProvider, SchemaStore {
   private val schemaSetSink = Sinks.many().replay().latest<SchemaSetChangedEvent>()
   override val schemaChanged: Publisher<SchemaSetChangedEvent> = schemaSetSink.asFlux()

   private val logger = KotlinLogging.logger {}

   private var lastSchemaSet: SchemaSet = SchemaSet.EMPTY

    override val schemaSet: SchemaSet
        get() {
            return lastSchemaSet
        }

   override val generation: Int
      get() {
         return schemaSet.generation
      }

   /**
    * Generates and emits SchemaSetChangedEvent if the schema is considered different.
    * If the schema is considered different, the internal state is updated.
    *
    * The event (which has already been emitted) is returned for convenience.
    */
   fun emitNewSchemaIfDifferent(newSchemaSet: SchemaSet): SchemaSetChangedEvent? {
      return SchemaSetChangedEvent.generateFor(lastSchemaSet, newSchemaSet)?.let { event ->
         logger.info("SchemaSet has been updated / created: $newSchemaSet - dispatching event.")
         lastSchemaSet = newSchemaSet
         schemaSetSink.emitNext(event, ContinueIfNonSerialHandler)
         event
      }
   }

}
