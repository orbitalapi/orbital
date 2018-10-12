package io.polymer.schemaStore

import io.osmosis.polymer.CompositeSchemaBuilder
import io.osmosis.polymer.schemas.Schema

interface SchemaSource {
   fun schemaStrings(): List<String>
   fun schemaString(): String = schemaStrings().joinToString("\n")

}
interface SchemaProvider {
   fun schemas(): List<Schema>
   fun schema(): Schema = CompositeSchemaBuilder().aggregate(schemas())

   companion object {
       fun from(schema:Schema):SchemaProvider {
          return object : SchemaProvider {
             override fun schemas(): List<Schema> = listOf(schema)
          }
       }
   }
}

interface SchemaSourceProvider : SchemaProvider, SchemaSource
