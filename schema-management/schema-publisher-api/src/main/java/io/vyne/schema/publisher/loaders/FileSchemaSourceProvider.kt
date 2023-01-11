package io.vyne.schema.publisher.loaders

import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.asSourcePackage
import io.vyne.asVersionedSource
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Ids
import lang.taxi.packages.ProjectName
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiPackageSources
import lang.taxi.packages.TaxiSourcesLoader
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * There's lots of dupcliation around the idea of loading taxi projects and files from disk.
 * This class is currently the preferred implementation.
 */
class FileSchemaSourceProvider(private val resourcePath: Path) : SchemaProvider {
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
         // Create a synthetic one, but tell the user off.
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
      TaxiSchema.from(this.packages)
   }
}
