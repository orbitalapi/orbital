package io.vyne.spring

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.asVersionedSource
import io.vyne.schemaStore.*
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.generators.java.TaxiGenerator
import lang.taxi.packages.TaxiSourcesLoader
import org.springframework.core.io.ClassPathResource
import java.nio.file.Path

class LocalResourceSchemaProvider(private val resourcePath: Path) : SchemaProvider {
   private val sources: List<VersionedSource> by lazy {
      TaxiSourcesLoader(resourcePath).load()
         .map { it.asVersionedSource() }
   }

   private val taxiSchema by lazy {
      TaxiSchema.from(this.sources)
   }

   private val taxiSchemaList by lazy {
      listOf(taxiSchema)
   }

   override fun schema(): Schema {
      return taxiSchema
   }

   override fun schemas(): List<Schema> {
      return taxiSchemaList
   }
}


class ClassPathSchemaSourceProvider(private val schemaFile: String) : SchemaSourceProvider {
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
      return classPathSchemaSourceProvider?.schemaStrings()
         ?: taxiGenerator.forClasses(models + services).generateAsStrings()
   }

   override fun schemas(): List<Schema> {
      return classPathSchemaSourceProvider?.schemas() ?: schemaStrings().map { TaxiSchema.from(it) }
   }
}

class RemoteTaxiSourceProvider(val schemaStore: SchemaStore) : SchemaSourceProvider, VersionedSourceProvider {
   override fun schemaStrings(): List<String> {
      return schemaStore.schemaSet().rawSchemaStrings
   }

   init {
      log().info("Initialized RemoteTaxiSchemaProvider, using a store client of type ${schemaStore.javaClass.simpleName}")
   }

   override val parsedSources: List<ParsedSource>
      get() {
         return schemaStore.schemaSet().sources
      }
   override val versionedSources: List<VersionedSource>
      get() {
         return schemaStore.schemaSet().allSources
      }

   override fun schemas(): List<Schema> {
      return schemaStore.schemaSet().taxiSchemas
   }

   override fun schema(): Schema {
      val schemaSet = schemaStore.schemaSet()
      return schemaSet.schema
   }


}
