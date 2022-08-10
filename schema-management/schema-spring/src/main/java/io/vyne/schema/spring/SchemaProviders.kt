package io.vyne.schema.spring

import io.vyne.*
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.publisher.loaders.FileSystemSourcesLoader
import io.vyne.schema.publisher.loaders.SchemaSourcesLoader
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Ids
import lang.taxi.packages.ProjectName
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiPackageSources
import lang.taxi.packages.TaxiSourcesLoader
import mu.KotlinLogging
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class FileSchemaSourceProvider(private val resourcePath: Path) : InternalSchemaSourceProvider, SchemaProvider {
   private val taxiProject: TaxiPackageSources

   private val logger = KotlinLogging.logger {}

   init {
      taxiProject = if (resourcePath.isRegularFile() && resourcePath.name == "taxi.conf") {
         // FIXME: When we add support for loading dependencies, this will need to provide a PackageLoader
         TaxiSourcesLoader.loadPackage(resourcePath.parent)
      } else if (resourcePath.isDirectory() && Files.exists(resourcePath.resolve("taxi.conf"))) {
         TaxiSourcesLoader.loadPackage(resourcePath)
      } else {
         logger.warn { "It is invalid to use a Taxi File loader, without a taxi.conf file.  A synthetic one has been created, but this will become a problem.  Define a taxi.conf file at $resourcePath" }
         // We weren't given a taxi project file.
         // Create a syntetic one, but tell the user off.
         val tempProject = TaxiPackageProject(
            name = ProjectName("com.fakeproject", Ids.id("project-")).id,
            version = "0.0.0",
            sourceRoot = "."
         )
         TaxiSourcesLoader.loadPackage(
            resourcePath,
            tempProject
         )
      }
   }

   override val packages: List<SourcePackage> by lazy {
      listOf(this.taxiProject.asSourcePackage())
   }

   override val versionedSources: List<VersionedSource> by lazy {
      taxiProject.sources.map { it.asVersionedSource() }
   }

   override val schema: Schema by lazy {
      TaxiSchema.from(this.versionedSources)
   }
}

interface InternalSchemaSourceProvider : SchemaSourceProvider


/**
 * Loads VersionedSources from paths.
 *
 * The real work is deferred to SchemaSourcesLoader implementations,
 * which allow smart things like transpilation from Swagger / Protobuf,
 * loading Taxi projects, and introspecting zip files / jars.
 */
class ProjectPathSchemaSourceProvider(
   private val loaders: List<SchemaSourcesLoader>
) : InternalSchemaSourceProvider {
   constructor(loader: SchemaSourcesLoader) : this(listOf(loader))
   constructor(path: Path) : this(FileSystemSourcesLoader(path))
   constructor(url: URL) : this(Paths.get(url.toURI()))

   override val versionedSources: List<VersionedSource>
      get() {

         // Design choice:
         // Here, we used to do a bunch of spring-specific work.
         // For now, leave that to the caller.
         return loaders.flatMap { loader ->
            loader.load()
         }
      }
}


// Source is mutable for testing
class SimpleTaxiSchemaProvider(var source: String, var identifer: PackageIdentifier) : SchemaProvider {
   companion object {
      fun from(
         source: String,
         identifier: PackageIdentifier = PackageIdentifier("com.fake", "Fake", "0.0.0")
      ): Pair<SimpleTaxiSchemaProvider, TaxiSchema> {
         val provider = SimpleTaxiSchemaProvider(source, identifier)
         return provider to provider.schema as TaxiSchema
      }
   }

   override val packages: List<SourcePackage>
      get() {
         return listOf(SourcePackage(PackageMetadata.from(identifer), sources = versionedSources))
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
