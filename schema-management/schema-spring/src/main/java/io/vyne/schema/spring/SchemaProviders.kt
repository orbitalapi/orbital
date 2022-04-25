package io.vyne.schema.spring

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.asVersionedSource
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.api.ParsedSourceProvider
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.publisher.loaders.SchemaSourcesLoader
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.packages.TaxiSourcesLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi

class FileSchemaSourceProvider(private val resourcePath: Path) : InternalSchemaSourceProvider, SchemaProvider {
   override val versionedSources: List<VersionedSource> by lazy {
      TaxiSourcesLoader(resourcePath).load()
         .map { it.asVersionedSource() }
   }

   override val schema: Schema by lazy {
      TaxiSchema.from(this.versionedSources)
   }
}

interface InternalSchemaSourceProvider : SchemaSourceProvider

interface TaxiProjectSourceProvider {
   val versionedSources: List<VersionedSource>
}

// projectPath can be in one of these formats:
// /var/opt/taxonomy
// /var/opt/foo.taxi
// TODO : This has been refactored, changing it's intent away from Spring wired in annotations
// to a more general concept.
// Does it still make sense?
// the projectPath is now redudnant, given the loader, (which used to be a class reference),
// however, do we even need this anymore?
data class LoadableSchemaProject(
   val loader: SchemaSourcesLoader,
)

class ProjectPathSchemaSourceProvider(
   private val projects: List<LoadableSchemaProject>,
   private val environment: ConfigurableEnvironment,
) : InternalSchemaSourceProvider {
   constructor(project: LoadableSchemaProject, environment: ConfigurableEnvironment) : this(
      listOf(project),
      environment
   )


   override val versionedSources: List<VersionedSource>
      get() {
         val resolver = PathMatchingResourcePatternResolver(this::class.java.classLoader)
         TODO("Do we still need this if we're reducing our dependency on Spring?")
//         return projects.flatMap { project ->
//            val loader = project.loader
//            var resources = resolver.getResources(project.projectPath).toList()
//            if (resources.size == 1 && environment.getProperty(project.projectPath) != null) {
//               resources = resolver.getResources(environment.getProperty(project.projectPath)).toList()
//            }
//            val paths = resources.map {
//               Paths.get(it.uri)
//            }
//            loader.load()
//         }




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


// Source is mutable for testing
class SimpleTaxiSchemaProvider(var source: String) : SchemaProvider {
   companion object {
      fun from(source: String): Pair<SimpleTaxiSchemaProvider, TaxiSchema> {
         val provider = SimpleTaxiSchemaProvider(source)
         return provider to provider.schema as TaxiSchema
      }
   }

   override val versionedSources: List<VersionedSource>
      get() {
         return listOf(VersionedSource.sourceOnly(source))
      }

   override val schema: TaxiSchema
      get() {
         return TaxiSchema.from(source)
      }

}

class SimpleSchemaSourceProvider(override val versionedSources: List<VersionedSource>) : SchemaSourceProvider {
}

/**
 * SchemaSourceProvider which generates taxi schemas from the provided classes
 */
//class AnnotationCodeGeneratingSchemaProvider(
//   val models: List<Class<*>>,
//   val services: List<Class<*>>,
//   val taxiGenerator: TaxiGenerator = TaxiGenerator()
//) : InternalSchemaSourceProvider {
//   private val schemaStrings: List<String>
//
//   init {
//      schemaStrings = if (models.isEmpty() && services.isEmpty()) {
//         emptyList()
//      } else {
//         taxiGenerator.forClasses(models + services).generateAsStrings()
//      }
//      log().info("Generated ${schemaStrings.size} schemas from ${models.size} models and ${services.size} services.  Enable debug logging to see the schema")
//      val generatedSchema = schemaStrings.joinToString("\n")
//      log().debug(generatedSchema)
//   }
//
//   override fun schemaStrings(): List<String> {
//      if (models.isEmpty() && services.isEmpty()) {
//         return emptyList()
//      }
//      return taxiGenerator.forClasses(models + services).generateAsStrings()
//   }
//
//   override fun schemas(): List<Schema> {
//      return schemaStrings().map { TaxiSchema.from(it) }
//   }
//}

class SchemaStoreSchemaProvider(val schemaStore: SchemaStore) : SchemaProvider, ParsedSourceProvider {
   init {
      log().info("Initialized SchemaStoreSchemaProvider, using a store client of type ${schemaStore.javaClass.simpleName}")
   }

   override val parsedSources: List<ParsedSource>
      get() {
         return schemaStore.schemaSet().sources
      }
   override val versionedSources: List<VersionedSource>
      get() {
         return schemaStore.schemaSet().allSources
      }

   override val schema: Schema
      get() {
         val schemaSet = schemaStore.schemaSet()
         return schemaSet.schema
      }

}

