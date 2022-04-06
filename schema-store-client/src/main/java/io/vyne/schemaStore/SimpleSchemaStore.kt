package io.vyne.schemaStore

import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.SchemaSetChangedEvent
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

/**
 * Basic schema store that simply holds the schema set provided to it.
 * Use where schema validation is deferred elsewhere - ie., in a remote model
 */
class SimpleSchemaStore : SchemaStore {
   private var schemaSet: SchemaSet = SchemaSet.EMPTY
   fun setSchemaSet(value: SchemaSet):SimpleSchemaStore {
      this.schemaSet = value;
      return this
   }

   override fun schemaSet(): SchemaSet {
      return schemaSet
   }

   override val generation: Int
      get() {
         return schemaSet.generation
      }
   override val schemaChanged: Publisher<SchemaSetChangedEvent>
      get() = Flux.empty()

}
