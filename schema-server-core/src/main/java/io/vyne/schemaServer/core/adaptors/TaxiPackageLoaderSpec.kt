package io.vyne.schemaServer.core.adaptors

import io.vyne.schema.api.PackageIdentifier
import io.vyne.schema.api.PackageMetadata
import io.vyne.schema.api.SchemaPackage
import io.vyne.schema.publisher.loaders.FileSystemSchemaProjectLoader
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schema.publisher.loaders.SchemaSourcesAdaptor
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
   override fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata> {
      return Mono.create { sink ->
         transport.listUris()
            .filter { uri ->
               File(uri).name == "taxi.conf"
            }
            .next()
            .subscribe { uri ->
               logger.info { "Reading taxi package file at $uri" }
               try {
                  val taxiFilePath = uri.toPath()
                  val project = TaxiProjectLoader().withConfigFileAt(taxiFilePath)
                     .load()
                  val packageIdentifier = FileBasedPackageMetadata(
                     identifier = project.identifier.toVynePackageIdentifier(),
                     dependencies = project.dependencyPackages.map { it.toVynePackageIdentifier() },
                     rootPath = taxiFilePath.parent
                  )
                  sink.success(packageIdentifier)
               } catch (e: Exception) {
                  logger.error(e) { "Failed to load a taxi project from $uri" }
                  sink.error(e)
               }
            }
      }
   }

   override fun convert(source: PackageMetadata, transport: SchemaPackageTransport): Mono<SchemaPackage> {
      require(source is FileBasedPackageMetadata) { "Currently, TaxiSchemaSourcesAdaptor expects a FileBasedPackageMetadata" }
      return Mono.create { sink ->
         try {
            val sources = FileSystemSchemaProjectLoader(source.rootPath).loadVersionedSources(
               forceVersionIncrement = false,
               cachedValuePermissible = true
            )
            sink.success(
               SchemaPackage(
                  source,
                  sources
               )
            )
         } catch (e: Exception) {
            logger.error(e) { "Exception when trying to build taxi project from source at ${source.rootPath}" }
            sink.error(e)
         }
      }
   }

}


fun lang.taxi.packages.PackageIdentifier.toVynePackageIdentifier(): PackageIdentifier {
   return PackageIdentifier(
      this.name.organisation,
      this.name.name,
      this.version.toString()
   )
}
