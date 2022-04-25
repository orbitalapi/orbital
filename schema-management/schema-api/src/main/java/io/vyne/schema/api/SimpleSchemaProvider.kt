package io.vyne.schema.api

import io.vyne.VersionedSource
import io.vyne.schemas.Schema

// schema is mutable to allow reconfiguration during testing
class SimpleSchemaProvider(override var schema: Schema) : SchemaProvider {
   override val versionedSources: List<VersionedSource>
      get() {
         return schema.sources
      }

}
