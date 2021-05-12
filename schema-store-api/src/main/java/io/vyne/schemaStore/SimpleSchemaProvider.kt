package io.vyne.schemaStore

import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema

/**
 * A simple provider which only serves the taxi source provided
 * as a SchemaProvider.
 *
 * Mainly useful in tests
 */
class SimpleTaxiSchemaProvider(val source: String) : SchemaSourceProvider {
   private val schemaStrings = listOf(source)
   private val schema: TaxiSchema = TaxiSchema.from(source)
   private val schemas = listOf(schema)

   companion object {
      fun from(source: String): Pair<SimpleTaxiSchemaProvider, TaxiSchema> {
         val provider = SimpleTaxiSchemaProvider(source)
         return provider to provider.schemas()[0] as TaxiSchema
      }
   }

   override fun schemaStrings(): List<String> = schemaStrings

   override fun schema(): Schema = schema
   override fun schemas(): List<Schema> = schemas

}
