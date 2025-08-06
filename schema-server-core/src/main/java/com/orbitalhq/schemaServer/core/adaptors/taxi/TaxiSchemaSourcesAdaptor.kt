package com.orbitalhq.schemaServer.core.adaptors.taxi

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.publisher.loaders.FileSchemaSourceProvider
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.toVynePackageIdentifier
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiProjectLoader
import mu.KotlinLogging
import org.taxilang.packagemanager.DependencyFetcherProvider
import org.taxilang.packagemanager.NoOpDependencyFetcherProvider
import reactor.core.publisher.Mono
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.toPath

class TaxiSchemaSourcesAdaptor(
   private val transpiler: TaxiSourceTranspiler = TaxiSourceTranspiler(),
   private val dependencyFetcherProvider: DependencyFetcherProvider = NoOpDependencyFetcherProvider
) : SchemaSourcesAdaptor {
   private val logger = KotlinLogging.logger {}


   fun loadTaxiProject(transport: SchemaPackageTransport): Mono<Pair<Path, TaxiPackageProject>> {

      return Mono.create { sink ->
         val rootPath = transport.root.toPath()
         // People tend to mix 'n' match passing the path to taxi.conf vs passing the directory
         // Just support either.
         val expectedTaxiConfPath = when {
            // Note: Don't do a isRegularFile() check here,
            // as that returns false if the file doesn't exist, which leads
            // to us double-appending taxi.conf to a path
            rootPath.fileName?.toString() == "taxi.conf" -> rootPath
            else -> transport.root.toPath().resolve(transport.config.pathToTaxiConf)
         }
         if (!expectedTaxiConfPath.exists()) {
            val errorMessage =
               "Error loading project from ${transport.description}: No taxi.conf file found at $expectedTaxiConfPath"
            logger.warn { errorMessage }
            sink.error(IllegalStateException(errorMessage))
         } else {
            logger.info { "Reading taxi package file at $expectedTaxiConfPath" }
            val project = TaxiProjectLoader(expectedTaxiConfPath)
               .load()
            sink.success(expectedTaxiConfPath to project)
         }
      }
   }

   override fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata> {
      return loadTaxiProject(transport)
         .map { (projectRoot, project) ->
            FileBasedPackageMetadata(
               identifier = project.identifier.toVynePackageIdentifier(),
               dependencies = project.dependencyPackages.map { it.toVynePackageIdentifier() },
               rootPath = projectRoot
            )
         }
   }

   override fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage> {
      require(packageMetadata is FileBasedPackageMetadata) { "TaxiSchemaSourcesAdaptor expects a FileBasedPackageMetadata" }
      return Mono.create { sink ->
         try {
            val sourcePackage = FileSchemaSourceProvider(packageMetadata.rootPath, dependencyFetcherProvider).packages.single()

            // If the taxi project declares transpiling sources
            // (eg., an OpenAPI spec, Avro Spec, etc),
            // we need to convert them to Taxi here.
            val packageWithTranspiledSources = transpiler.transpileAndCombineSources(sourcePackage)
            sink.success(packageWithTranspiledSources)
         } catch (e: Exception) {
            logger.error(e) { "Exception when trying to build taxi project from source at ${packageMetadata.rootPath}" }
            sink.error(e)
         }
      }
   }

}
