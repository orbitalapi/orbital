package io.vyne.schemaServer.core.adaptors.taxi

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schema.publisher.loaders.SchemaSourcesAdaptor
import io.vyne.schema.publisher.loaders.FileSchemaSourceProvider
import io.vyne.schemaServer.core.adaptors.PackageLoaderSpec
import io.vyne.schemaServer.core.adaptors.PackageType
import io.vyne.toVynePackageIdentifier
import lang.taxi.packages.TaxiPackageProject
import lang.taxi.packages.TaxiProjectLoader
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.toPath

/**
 * Placeholder, since all the metadata info is available in the spec we load
 * from the source.
 */
object TaxiPackageLoaderSpec : PackageLoaderSpec {
   override val packageType: PackageType = PackageType.Taxi
}

data class FileBasedPackageMetadata(
   override val identifier: PackageIdentifier,

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    */
   override val submissionDate: Instant = Instant.now(),
   override val dependencies: List<PackageIdentifier> = emptyList(),
   val rootPath: Path
) : PackageMetadata

class TaxiSchemaSourcesAdaptor : SchemaSourcesAdaptor {
   private val logger = KotlinLogging.logger {}

   fun loadTaxiProject(transport: SchemaPackageTransport): Mono<Pair<Path, TaxiPackageProject>> {
      return transport.listUris()
         .filter { File(it).name == "taxi.conf" }
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