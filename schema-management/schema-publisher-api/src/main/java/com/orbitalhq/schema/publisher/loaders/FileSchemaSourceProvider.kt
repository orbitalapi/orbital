package com.orbitalhq.schema.publisher.loaders

import com.orbitalhq.*
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.Ids
import lang.taxi.packages.*
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
   override val additionalSources: List<Pair<SourcesType, PathGlob>>
      get() {
         return this.taxiProject.pathGlobs()
      }

//   override val additionalSources: List<Pair<String, Path>>
//      get() {
//         return taxiProject.project.additionalSources.toList()
//      }

   override val schema: Schema by lazy {
      TaxiSchema.from(this.packages)
   }
}
