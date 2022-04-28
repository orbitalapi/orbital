package io.vyne.schemaStore

import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.SchemaSetChangedEvent
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

/**
 * Used for testing.
 */
class SimpleSchemaStore(
   override var schemaSet: SchemaSet = SchemaSet.EMPTY
) : SchemaStore {

   override val generation: Int
      get() {
         return schemaSet.generation
      }
   override val schemaChanged: Publisher<SchemaSetChangedEvent>
      get() = Flux.empty()

   fun setSchemaSet(schemaSet: SchemaSet):SimpleSchemaStore {
      this.schemaSet = schemaSet
      return this
   }
}
