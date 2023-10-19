package com.orbitalhq.schemaServer.core.packages

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.ParsedPackage
import com.orbitalhq.UriSafePackageIdentifier
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schema.publisher.ExpiringSourcesStore
import com.orbitalhq.schema.api.PublisherType
import com.orbitalhq.schema.api.SchemaPackageTransport
import com.orbitalhq.schemaServer.core.git.GitRepositorySpec
import com.orbitalhq.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import com.orbitalhq.schemaServer.packages.PackageWithDescription
import com.orbitalhq.schemaServer.packages.PackagesServiceApi
import com.orbitalhq.schemaServer.packages.SourcePackageDescription
import com.orbitalhq.schemas.DefaultPartialSchema
import com.orbitalhq.schemas.PartialSchema
import com.orbitalhq.spring.http.NotFoundException
import mu.KotlinLogging
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


@RestController
class PackageService(
   private val expiringSourcesStore: ExpiringSourcesStore,
   private val schemaStore: SchemaStore,
   private val repositoryManager: ReactiveRepositoryManager,
   private val configRepo: SchemaRepositoryConfigLoader
) : PackagesServiceApi {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @GetMapping("/api/packages/{packageUri}")
   override fun loadPackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<PackageWithDescription> {
      val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)
      val sourcePackage = schemaStore.schemaSet.parsedPackages.firstOrNull { it.identifier == packageIdentifier }
         ?: throw NotFoundException("Package $packageIdentifier was not found on this server")

      val packageDescription = buildPackageDescription(sourcePackage)
      return Mono.just(
         PackageWithDescription(
            parsedPackage = sourcePackage,
            description = packageDescription
         )
      )
   }

   @DeleteMapping("/api/packages/{packageUri}")
   override fun removePackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<Unit> {
      logger.info { "Received request to delete source package $packageUri" }
      return loadPackage(packageUri)
         .map { packageWithDescription ->
            val packageDescription = packageWithDescription.description
            when (packageDescription.publisherType) {
               PublisherType.GitRepo -> {
                  val repositoryName = (packageDescription.packageConfig as GitRepositorySpec).name
                  configRepo.removeGitRepository(repositoryName, packageDescription.identifier)
               }

               PublisherType.FileSystem -> {
                  configRepo.removeFileRepository(packageDescription.identifier)
               }

               else -> {
                  error("Removing packages is not supported for publisher type ${packageDescription.publisherType}")
               }
            }
         }
   }

   private fun buildEmptyDescription(packageTransport: SchemaPackageTransport): SourcePackageDescription {
      val identifier = packageTransport.packageIdentifier
      return SourcePackageDescription(
         identifier,
         expiringSourcesStore.getPublisherHealth(identifier),
         0,
         0, // TODO : Warning count
         0,
         packageTransport.publisherType,
         packageTransport.isEditable(),
         packageTransport.config
      )
   }

   @GetMapping("/api/packages")
   override fun listPackages(): Mono<List<SourcePackageDescription>> {
      val packages = schemaStore.schemaSet.parsedPackages.map { parsedPackage ->
         buildPackageDescription(parsedPackage)
      }

      // Edge case: Find any loaders that are configured, but don't yet have any sources.
      // These won't be present in the above, since they don't contribute any parsed packages.
      val foundPackages = packages.map { it.identifier }.toSet()
      val emptyPackages = repositoryManager.loaders.filter { loader ->
         !foundPackages.contains(loader.packageIdentifier)
      }.map { buildEmptyDescription(it) }
      return Mono.just(packages)
   }

   private fun buildPackageDescription(parsedPackage: ParsedPackage): SourcePackageDescription {
      val loader = repositoryManager.getLoaderOrNull(parsedPackage.identifier)
      val publisherType = loader?.publisherType ?: PublisherType.Pushed
      val editable = loader?.isEditable() ?: false

      return SourcePackageDescription(
         parsedPackage.identifier,
         expiringSourcesStore.getPublisherHealth(parsedPackage.identifier),
         parsedPackage.sources.size,
         0, // TODO : Warning count
         parsedPackage.sourcesWithErrors.size,
         publisherType,
         editable,
         loader?.config
      )
   }

   @GetMapping("/api/packages/{packageUri}/schema")
   override fun getPartialSchemaForPackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<PartialSchema> {
      val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)

      // This is a brute-force approach, since we don't currently store a reference of schema members to the
      // sources they came from.
      val sourcePackage = schemaStore.schemaSet.packages.firstOrNull { it.identifier == packageIdentifier }
         ?: throw NotFoundException("No package with id $packageUri is present in the sources")

      val types = schemaStore.schemaSet.schema.types
         .filter { it.sources.any { source -> source.packageIdentifier == sourcePackage.identifier } }
      val services = schemaStore.schemaSet.schema.services
         .filter { it.sourceCode.any { source -> source.packageIdentifier == sourcePackage.identifier } }
      return Mono.just(
         DefaultPartialSchema(
            types.toSet(),
            services.toSet()
         )
      )
   }
}
