package com.orbitalhq.schema.consumer

import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.SchemaSetChangedEvent
import org.reactivestreams.Publisher

/**
 * Responsible for storing the retrieved schemas
 * and storing locally for usage.
 *
 * There's overlapping concerns here between SchemaStore and SchemaProvider.
 * Current opinion, weakly held:
 * Use a SchemaProvider by default, if you just need to ask for a schema.
 * If you need an event stream that tells you when schemas have changed, use a SchemaStore.
 *
 * Over time, we should converge these two ideas (towards SchemaProvider).
 *
 * Note: Not a huge amount of digging done on this comment, so if this turns out to be wrong,
 * docuement it here.
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
