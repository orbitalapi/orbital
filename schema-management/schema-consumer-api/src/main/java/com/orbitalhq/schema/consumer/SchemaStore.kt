package com.orbitalhq.schema.consumer

import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.schema.api.SchemaProvider
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

// Introduced for testing, but might make sense overall, given the above comments
class SchemaStoreToSchemaProviderWrapper(private val store: SchemaStore) : SchemaProvider {
   override val packages: List<SourcePackage>
      get() {
         return store.schemaSet.packages
      }
   @Deprecated("use packages instead")
   override val versionedSources: List<VersionedSource>
      get() {
         return store.schemaSet.allSources
      }

   override val schema: Schema
      get() {
         return store.schema()
      }

}

interface SchemaChangedEventProvider {
   val schemaChanged: Publisher<SchemaSetChangedEvent>

   /**
    * Forces pushing the current state as a schema change event.
    * Normally, you shouldn't need this, as changes are detected automatically.
    */
   fun forceSchemaChangedEvent()
}
