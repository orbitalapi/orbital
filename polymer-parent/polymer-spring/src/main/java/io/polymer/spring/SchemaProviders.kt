package io.polymer.spring

import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import io.osmosis.polymer.utils.log
import io.polymer.schemaStore.SchemaStoreClient
import lang.taxi.generators.java.TaxiGenerator

interface SchemaProvider {
   fun schemaStrings(): List<String>
   fun schemaString(): String = schemaStrings().joinToString("\n")
   fun schemas(): List<Schema>
}

class LocalTaxiSchemaProvider(val models: List<Class<*>>, val services: List<Class<*>>) : SchemaProvider {
   override fun schemaStrings(): List<String> {
      return TaxiGenerator().forClasses(models + services).generateAsStrings()
   }

   override fun schemas(): List<Schema> {
      return schemaStrings().map { TaxiSchema.from(it) }
   }
}

class RemoteTaxiSchemaProvider(val storeClient: SchemaStoreClient) : SchemaProvider {

   private var currentSchemaSet = storeClient.schemaSet
   private var schemas: List<Schema> = emptyList()
   override fun schemas(): List<Schema> {
      // Cache the schemas until the upstream schema set changes
      if (storeClient.schemaSet != currentSchemaSet) {
         this.currentSchemaSet = storeClient.schemaSet
         log().debug("Rebuilding schemas based on SchemaSet ${this.currentSchemaSet.id}")
         this.schemas = schemaStrings().map { TaxiSchema.from(it) }
      }
      return this.schemas
   }

   override fun schemaStrings(): List<String> {
      return storeClient.schemaSet.schemas.map { it.content }
   }

}
