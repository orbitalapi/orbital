package io.vyne.schema.consumer

import io.vyne.schema.api.SchemaSet
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import org.reactivestreams.Publisher

/**
 * Responsible for storing the retrieved schemas
 * and storing locally for usage
 */
interface SchemaStore: SchemaChangedEventProvider {
   val schemaSet: SchemaSet
   val generation: Int

   fun schema(): Schema {
      return this.schemaSet.schema
   }
}

interface SchemaChangedEventProvider {
   val schemaChanged: Publisher<SchemaSetChangedEvent>
}
