package com.orbitalhq.schemaServer.core.adaptors.taxi

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.publisher.loaders.FileSchemaSourceProvider
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.avro.AvroTaxiSourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.openapi.FileLoadingOpenApiSpecProvider
import com.orbitalhq.schemaServer.core.adaptors.openapi.OpenApiSourceGenerator
import com.orbitalhq.schemas.taxi.mergeLists
import com.orbitalhq.toVynePackageIdentifier
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiProjectLoader
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.toPath

class TaxiSchemaSourcesAdaptor(
   private val sourceGenerators:List<SourceGenerator> = DEFAULT_SOURCE_GENERATORS
) : SchemaSourcesAdaptor {
   private val logger = KotlinLogging.logger {}

   companion object {
      val DEFAULT_SOURCE_GENERATORS = listOf(
         AvroTaxiSourceGenerator(),
         OpenApiSourceGenerator(FileLoadingOpenApiSpecProvider())
      )
   }
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
            val sourcePackage = FileSchemaSourceProvider(packageMetadata.rootPath).packages.single()

            // If the taxi project declares transpiling sources
            // (eg., an OpenAPI spec, Avro Spec, etc),
            // we need to convert them to Taxi here.
            val transpiledSourcePackages = transpileAdditionalSources(packageMetadata, sourcePackage)
            val combinedSources = combineSourcePackages(sourcePackage, transpiledSourcePackages)
            sink.success(combinedSources)
         } catch (e: Exception) {
            logger.error(e) { "Exception when trying to build taxi project from source at ${packageMetadata.rootPath}" }
            sink.error(e)
         }
      }
   }

   private fun combineSourcePackages(primarySourcePackage: SourcePackage, otherSourcePackages: List<SourcePackage>):SourcePackage {
      if (otherSourcePackages.isEmpty()) {
         return primarySourcePackage
      }
      return otherSourcePackages.fold(primarySourcePackage) { a,b ->
         val mergedSources = a.sources + b.sources
         val mergedAdditionalSources = a.additionalSources.mergeLists(b.additionalSources)
         a.copy(
            sources = mergedSources,
            additionalSources = mergedAdditionalSources
         )
      }
   }

   private fun transpileAdditionalSources(
      packageMetadata: FileBasedPackageMetadata,
      sourcePackage: SourcePackage
   ): List<SourcePackage> {
      return sourcePackage.additionalSources
         .mapNotNull { (sourceType, sources) ->
            val generator = sourceGenerators.firstOrNull {
               it.supportsSources(sourceType)
            } ?: return@mapNotNull null
            generator.generateSourcePackage(sources, packageMetadata)
         }
   }
}
