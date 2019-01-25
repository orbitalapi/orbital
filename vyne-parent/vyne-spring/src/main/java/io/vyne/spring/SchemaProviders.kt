package io.vyne.spring

import io.vyne.schemaStore.*
import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.NamedSource
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

   init {
      log().info("Initialized RemoteTaxiSchemaProvider, using a store client of type ${storeClient.javaClass.simpleName}")
   }

   override val versionedSchemas: List<VersionedSchema>
      get() {
         return activeSchemaSet().schemas
      }

   private var lastBuildSchemaSet = SchemaSet.EMPTY
   private var schemas: List<Schema> = emptyList()

   private fun activeSchemaSet(): SchemaSet {
      rebuildCacheIfRequired()
      return this.lastBuildSchemaSet
   }

   private fun rebuildCacheIfRequired() {
      if (storeClient.schemaSet() != lastBuildSchemaSet) {
         this.lastBuildSchemaSet = storeClient.schemaSet()
         log().debug("Rebuilding schemas based on SchemaSet ${this.lastBuildSchemaSet.id}")
         val namedSources = schemasByName().map { (name, content) -> NamedSource(content,name) }
         this.schemas = TaxiSchema.from(namedSources)
      }
   }

   override fun schemas(): List<Schema> {
      // Cache the schemas until the upstream schema set changes
      rebuildCacheIfRequired()
      return this.schemas
   }

   override fun schema(): Schema {
      val sources = this.activeSchemaSet().schemas.map { NamedSource(it.content, it.id) }
      return CompositeSchema(TaxiSchema.from(sources))
   }

   private fun schemasByName(): Map<String, String> {
      return storeClient.schemaSet().schemas.map { it.name to it.content }.toMap()
   }

   override fun schemaStrings(): List<String> {
      return storeClient.schemaSet().schemas.map { it.content }
   }

}
