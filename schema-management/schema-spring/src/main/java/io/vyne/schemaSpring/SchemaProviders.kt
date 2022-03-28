package io.vyne.schemaSpring

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.asVersionedSource
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemaApi.SchemaSourceProvider
import io.vyne.schemaApi.VersionedSourceProvider
import io.vyne.schemaConsumerApi.SchemaStore
import io.vyne.schemaPublisherApi.loaders.FileSystemSchemaProjectLoader
import io.vyne.schemaPublisherApi.loaders.SchemaSourcesLoader
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.generators.java.TaxiGenerator
import lang.taxi.packages.TaxiSourcesLoader
import lang.taxi.sources.SourceCode
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

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

interface InternalSchemaSourceProvider : SchemaSourceProvider

interface TaxiProjectSourceProvider {
   fun versionedSources(): List<VersionedSource>
}

// projectPath can be in one of these formats:
// /var/opt/taxonomy
// /var/opt/foo.taxi
data class LoadableSchemaProject(
   val projectPath: String,
   val loaderClass: Class<out SchemaSourcesLoader>,
)

class ProjectPathSchemaSourceProvider(
   private val projects: List<LoadableSchemaProject>,
   private val environment: ConfigurableEnvironment,
) : InternalSchemaSourceProvider, TaxiProjectSourceProvider {
   constructor(project: LoadableSchemaProject, environment: ConfigurableEnvironment) : this(
      listOf(project),
      environment
   )

   override fun schemas(): List<Schema> {
      return listOf(TaxiSchema.from(versionedSources()))
   }

   override fun schemaStrings(): List<String> {
      return versionedSources().map { it.content }
   }

   @OptIn(ExperimentalPathApi::class)
   override fun versionedSources(): List<VersionedSource> {
      val resolver = PathMatchingResourcePatternResolver(this::class.java.classLoader)
      return projects.flatMap { project ->
         val loader = project.loaderClass.getConstructor().newInstance()
         val resources = resolver.getResources(project.projectPath).toList()
         val paths = resources.map {
            Paths.get(it.uri)
         }
         loader.load(paths)
      }
//      val path = resolvePath()
//
//      return when {
//         path.isDirectory() -> {
//            val fileSystemVersionedSourceLoader = FileSystemSchemaProjectLoader(path)
//            fileSystemVersionedSourceLoader.loadVersionedSources(forceVersionIncrement = false, cachedValuePermissible = false)
//         }
//         else -> {
//            FileBasedSchemaSourceProvider(schemaFile = projectPath)
//               .schemaStrings()
//               .map {
//                  VersionedSource.fromTaxiSourceCode(SourceCode(path.toFile().name, it, path))
//               }
//         }
//      }
   }
}

class FileBasedSchemaSourceProvider(private val schemaFile: String) : InternalSchemaSourceProvider {
   override fun schemas() = schemaStrings().map { TaxiSchema.from(it) }
   override fun schemaStrings(): List<String> {
      return listOf(File(schemaFile).inputStream().bufferedReader(Charsets.UTF_8).readText())
   }
}

// Source is mutable for testing
class SimpleTaxiSchemaProvider(var source: String) : SchemaSourceProvider {
   companion object {
      fun from(source: String): Pair<SimpleTaxiSchemaProvider, TaxiSchema> {
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
class AnnotationCodeGeneratingSchemaProvider(
   val models: List<Class<*>>,
   val services: List<Class<*>>,
   val taxiGenerator: TaxiGenerator = TaxiGenerator()
) : InternalSchemaSourceProvider {
   private val schemaStrings: List<String>

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

