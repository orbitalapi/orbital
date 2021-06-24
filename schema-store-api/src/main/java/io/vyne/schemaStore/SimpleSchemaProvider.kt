package io.vyne.schemaStore

import io.vyne.schemas.Schema

class SimpleSchemaProvider(val schema: Schema) : SchemaProvider {
   override fun schemas(): List<Schema> {
      return listOf(schema)
   }

   override fun schema(): Schema {
      return schema
   }
}
