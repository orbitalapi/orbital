package io.vyne.schemaStore

import io.vyne.schemas.Schema

// schema is mutable to allow reconfiguration during testing
class SimpleSchemaProvider(var schema: Schema) : SchemaProvider {
   override fun schemas(): List<Schema> {
      return listOf(schema)
   }

   override fun schema(): Schema {
      return schema
   }
}
