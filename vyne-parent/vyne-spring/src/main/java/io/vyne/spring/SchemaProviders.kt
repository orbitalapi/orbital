package io.vyne.spring

import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemaStore.VersionedSchema
import io.vyne.schemaStore.VersionedSchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.generators.java.TaxiGenerator


class SimpleTaxiSchemaProvider(val source: String) : SchemaSourceProvider {
   override fun schemaStrings(): List<String> {
      return listOf(source)
   }

   override fun schemas(): List<Schema> {
      return listOf(TaxiSchema.from(source))
   }

}

class LocalTaxiSchemaProvider(val models: List<Class<*>>, val services: List<Class<*>>, val taxiGenerator: TaxiGenerator = TaxiGenerator()) : SchemaSourceProvider {
   override fun schemaStrings(): List<String> {
      return taxiGenerator.forClasses(models + services).generateAsStrings()
   }

   override fun schemas(): List<Schema> {
      return schemaStrings().map { TaxiSchema.from(it) }
   }
}

class RemoteTaxiSchemaProvider(val storeClient: SchemaStoreClient) : SchemaSourceProvider, VersionedSchemaProvider {
   override fun schemaStrings(): List<String> {
      return storeClient.schemaSet().rawSchemaStrings
   }

   init {
      log().info("Initialized RemoteTaxiSchemaProvider, using a store client of type ${storeClient.javaClass.simpleName}")
   }

   override val versionedSchemas: List<VersionedSchema>
      get() {
         return storeClient.schemaSet().sources
      }


   override fun schemas(): List<Schema> {
      return storeClient.schemaSet().taxiSchemas
   }

   override fun schema(): Schema {
      val schemaSet = storeClient.schemaSet()
      return schemaSet.schema
   }


}
