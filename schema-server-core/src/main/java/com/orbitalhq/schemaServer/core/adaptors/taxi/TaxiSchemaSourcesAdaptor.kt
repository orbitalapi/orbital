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
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Path
import kotlin.io.path.toPath

class TaxiSchemaSourcesAdaptor : SchemaSourcesAdaptor {
   private val logger = KotlinLogging.logger {}

   fun loadTaxiProject(transport: SchemaPackageTransport): Mono<Pair<Path, TaxiPackageProject>> {
      return transport.listUris()
         .filter { File(it).name == "taxi.conf" }
         .switchIfEmpty {
            error("No taxi.conf file found at root of ${transport.description}")
         }
         .next()
         .map { uri ->
            logger.info { "Reading taxi package file at $uri" }
            val taxiFilePath = uri.toPath()
            val project = TaxiProjectLoader().withConfigFileAt(taxiFilePath)
               .load()
            taxiFilePath to project
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
      require(packageMetadata is FileBasedPackageMetadata) { "Currently, TaxiSchemaSourcesAdaptor expects a FileBasedPackageMetadata" }
      return Mono.create { sink ->
          try {
              val sourcePackage = FileSchemaSourceProvider(packageMetadata.rootPath).packages.single()
              sink.success(sourcePackage)
          } catch (e: Exception) {
              logger.error(e) { "Exception when trying to build taxi project from source at ${packageMetadata.rootPath}" }
              sink.error(e)
          }
      }
   }

}
