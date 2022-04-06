package io.vyne.schema.consumer

import io.vyne.schema.api.SchemaSet
import io.vyne.schemas.SchemaSetChangedEvent
import lang.taxi.utils.log
import org.reactivestreams.Publisher
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks

abstract class SchemaSetChangedEventRepository: SchemaChangedEventProvider {
   private val schemaSetSink = Sinks.many().replay().latest<SchemaSetChangedEvent>()
   override val schemaChanged: Publisher<SchemaSetChangedEvent> = schemaSetSink.asFlux()
   private val emitFailureHandler = Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
      (emitResult
         == Sinks.EmitResult.FAIL_NON_SERIALIZED)
   }
   fun publishSchemaSetChangedEvent(
       oldSchemaSet: SchemaSet?,
       newSchemaSet: SchemaSet,
       schemaSetCallback: (schemaSet: SchemaSet) -> Unit = {}) {
      SchemaSetChangedEvent.generateFor(oldSchemaSet, newSchemaSet)?.let {
         log().info("SchemaSet has been updated / created: $newSchemaSet - dispatching event.")
         schemaSetCallback(it.newSchemaSet)
         schemaSetSink.emitNext(it, emitFailureHandler)
      }
   }
}
