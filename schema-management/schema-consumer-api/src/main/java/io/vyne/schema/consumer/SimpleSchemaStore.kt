package io.vyne.schema.consumer

import io.vyne.schema.api.SchemaSet
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import org.reactivestreams.Publisher
import reactor.core.publisher.Sinks

/**
 * Used for testing.
 */
class SimpleSchemaStore(
   override var schemaSet: SchemaSet = SchemaSet.EMPTY
) : SchemaStore {
   private val schemaChangedSink = Sinks.many().multicast().directBestEffort<SchemaSetChangedEvent>()

   override val generation: Int
      get() {
         return schemaSet.generation
      }
   override val schemaChanged: Publisher<SchemaSetChangedEvent>
      get() = schemaChangedSink.asFlux()

   fun setSchema(schema:Schema):SchemaSet {
      val schemaSet = SchemaSet.from(schema, this.schemaSet.generation + 1)
      setSchemaSet(schemaSet)
      return  schemaSet
   }
   fun setSchemaSet(schemaSet: SchemaSet): SimpleSchemaStore {
      val oldSchemaSet = this.schemaSet
      this.schemaSet = schemaSet
      schemaChangedSink.emitNext(SchemaSetChangedEvent(oldSchemaSet, schemaSet), Sinks.EmitFailureHandler.FAIL_FAST)
      return this
   }
}
