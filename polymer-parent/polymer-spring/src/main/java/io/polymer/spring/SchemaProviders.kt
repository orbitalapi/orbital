package io.polymer.spring

import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import io.osmosis.polymer.utils.log
import io.polymer.schemaStore.SchemaSet
import io.polymer.schemaStore.SchemaSourceProvider
import io.polymer.schemaStore.SchemaStoreClient
import lang.taxi.generators.java.TaxiGenerator


class LocalTaxiSchemaProvider(val models: List<Class<*>>, val services: List<Class<*>>, val taxiGenerator: TaxiGenerator = TaxiGenerator()) : SchemaSourceProvider {
   override fun schemaStrings(): List<String> {
      return taxiGenerator.forClasses(models + services).generateAsStrings()
   }

   override fun schemas(): List<Schema> {
      return schemaStrings().map { TaxiSchema.from(it) }
   }
}

class RemoteTaxiSchemaProvider(val storeClient: SchemaStoreClient) : SchemaSourceProvider {

   init {
      log().info("Initialized RemoteTaxiSchemaProvider, using a store client of type ${storeClient.javaClass.simpleName}")
   }

   private var currentSchemaSet =SchemaSet.EMPTY
   private var schemas: List<Schema> = emptyList()
   override fun schemas(): List<Schema> {
      // Cache the schemas until the upstream schema set changes
      if (storeClient.schemaSet() != currentSchemaSet) {
         this.currentSchemaSet = storeClient.schemaSet()
         log().debug("Rebuilding schemas based on SchemaSet ${this.currentSchemaSet.id}")
         this.schemas = schemasByName().map { (name,content) -> TaxiSchema.from(content,name) }
      }
      return this.schemas
   }

   private fun schemasByName():Map<String,String> {
      return storeClient.schemaSet().schemas.map { it.name to it.content }.toMap()
   }

   override fun schemaStrings(): List<String> {
      return storeClient.schemaSet().schemas.map { it.content }
   }

}
