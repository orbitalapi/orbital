package io.vyne.spring

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemaStore.VersionedSourceProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.generators.java.TaxiGenerator
import org.springframework.core.io.ClassPathResource

class ClassPathSchemaSourceProvider(private val schemaFile: String): SchemaSourceProvider {
   override fun schemas() = schemaStrings().map { TaxiSchema.from(it) }
   override fun schemaStrings() = listOf(ClassPathResource(schemaFile).inputStream.bufferedReader(Charsets.UTF_8).readText())
}

class SimpleTaxiSchemaProvider(val source: String) : SchemaSourceProvider {
   override fun schemaStrings(): List<String> {
      return listOf(source)
   }

   override fun schemas(): List<Schema> {
      return listOf(TaxiSchema.from(source))
   }

}

class LocalTaxiSchemaProvider(val models: List<Class<*>>,
                              val services: List<Class<*>>,
                              val taxiGenerator: TaxiGenerator = TaxiGenerator(),
                              val classPathSchemaSourceProvider: ClassPathSchemaSourceProvider? = null) : SchemaSourceProvider {
   override fun schemaStrings(): List<String> {
      return classPathSchemaSourceProvider?.schemaStrings() ?: taxiGenerator.forClasses(models + services).generateAsStrings()
   }

   override fun schemas(): List<Schema> {
      return classPathSchemaSourceProvider?.schemas() ?: schemaStrings().map { TaxiSchema.from(it) }
   }
}

class RemoteTaxiSourceProvider(val storeClient: SchemaStoreClient) : SchemaSourceProvider, VersionedSourceProvider {
   override fun schemaStrings(): List<String> {
      return storeClient.schemaSet().rawSchemaStrings
   }

   init {
      log().info("Initialized RemoteTaxiSchemaProvider, using a store client of type ${storeClient.javaClass.simpleName}")
   }

   override val parsedSources: List<ParsedSource>
      get() {
         return storeClient.schemaSet().sources
      }
   override val versionedSources: List<VersionedSource>
      get() {
         return storeClient.schemaSet().allSources
      }

   override fun schemas(): List<Schema> {
      return storeClient.schemaSet().taxiSchemas
   }

   override fun schema(): Schema {
      val schemaSet = storeClient.schemaSet()
      return schemaSet.schema
   }


}
