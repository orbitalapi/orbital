package io.vyne.schemaSpring

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.asVersionedSource
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemaApi.SchemaSourceProvider
import io.vyne.schemaApi.VersionedSourceProvider
import io.vyne.schemaConsumerApi.SchemaStore
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.generators.java.TaxiGenerator
import lang.taxi.packages.TaxiSourcesLoader
import lang.taxi.sources.SourceCode
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
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

interface InternalSchemaSourceProvider: SchemaSourceProvider
// projectPath can be in one of these formats:
// classpath:folder
// classpath:foo.taxi
// filepath:folder
// filepath:foo.taxi
class ProjectPathSchemaSourceProvider(
   private var projectPath: String,
   private val environment: ConfigurableEnvironment):  InternalSchemaSourceProvider {
   override fun schemas(): List<Schema> {
      return listOf(TaxiSchema.from(versionedSources().sources))
   }

   override fun schemaStrings(): List<String> {
      return versionedSources().sources.map { it.content }
   }

   private fun versionedSources() : VersionedSourceSubmission {
      val resource = getResource()
      return if (resource.file.isDirectory) {
         val fileSystemVersionedSourceLoader = FileSystemSchemaLoader(resource.file.toPath())
         fileSystemVersionedSourceLoader.loadVersionedSources(false, false)
      } else {
         val sources = FileBasedSchemaSourceProvider(schemaFile = null, schemaFileResource = resource)
            .schemaStrings().map { VersionedSource.fromTaxiSourceCode(SourceCode(resource.filename, it)) }
         VersionedSourceSubmission(sources, resource.filename)
      }
   }

   private fun getResource(): Resource {
      return when {
         projectPath.startsWith(ClassPathTaxonomy) -> PathMatchingResourcePatternResolver().getResource(projectPath)
         projectPath.startsWith(FilePathTaxonomy) -> FileSystemResourceLoader().getResource(projectPath)
         else -> {
            val projectPathVal = environment.getProperty(projectPath)
               ?: throw IllegalArgumentException("$projectPath should either start with classpath: or file: or specifies a property with a value that starts with classpath: or file:")

            if (!projectPathVal.startsWith(ClassPathTaxonomy) && !projectPathVal.startsWith(FilePathTaxonomy)) {
               throw IllegalArgumentException("$projectPath should either start with classpath: or file: or specifies a property with a value that starts with classpath: or file:")
            }
            else {
               projectPath = projectPathVal
               getResource()
            }
         }
      }
   }

   companion object {
      private const val ClassPathTaxonomy = "classpath:"
      private const val FilePathTaxonomy = "file:"
   }

}

class FileBasedSchemaSourceProvider(private val schemaFile: String?, private val schemaFileResource: Resource? = null) :  InternalSchemaSourceProvider {
   override fun schemas() = schemaStrings().map { TaxiSchema.from(it) }
   override fun schemaStrings(): List<String> {
      return schemaFile?.let {
         listOf(ClassPathResource(schemaFile).inputStream.bufferedReader(Charsets.UTF_8).readText())
      } ?:
      schemaFileResource?.let {
         listOf(it.inputStream.bufferedReader(Charsets.UTF_8).readText())
      } ?: listOf()
   }
}

// Source is mutable for testing
class SimpleTaxiSchemaProvider(var source: String) : SchemaSourceProvider {
   companion object {
      fun from(source:String):Pair<SimpleTaxiSchemaProvider, TaxiSchema> {
         val provider = SimpleTaxiSchemaProvider(source)
         return provider to provider.schemas()[0] as TaxiSchema
      }
   }
   override fun schemaStrings(): List<String> {
      return listOf(source)
   }

   override fun schemas(): List<Schema> {
      return listOf(TaxiSchema.from(source))
   }

}

class VersionedSchemaProvider(private val sources: List<VersionedSource>) : SchemaSourceProvider {
   override fun schemaStrings(): List<String> {
      return sources.map { it.content }
   }
   override fun schemas(): List<Schema> {
      return listOf(TaxiSchema.from(sources))
   }
}

/**
 * SchemaSourceProvider which generates taxi schemas from the provided classes
 */
class AnnotationCodeGeneratingSchemaProvider(val models: List<Class<*>>,
                                             val services: List<Class<*>>,
                                             val taxiGenerator: TaxiGenerator = TaxiGenerator()) :  InternalSchemaSourceProvider {
   private val schemaStrings:List<String>
   init {
      schemaStrings = if (models.isEmpty() && services.isEmpty()) {
         emptyList()
      } else {
         taxiGenerator.forClasses(models + services).generateAsStrings()
      }
      log().info("Generated ${schemaStrings.size} schemas from ${models.size} models and ${services.size} services.  Enable debug logging to see the schema")
      val generatedSchema = schemaStrings.joinToString("\n")
      log().debug(generatedSchema)
   }
   override fun schemaStrings(): List<String> {
      if (models.isEmpty() && services.isEmpty()) {
         return emptyList()
      }
      return taxiGenerator.forClasses(models + services).generateAsStrings()
   }

   override fun schemas(): List<Schema> {
      return schemaStrings().map { TaxiSchema.from(it) }
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

