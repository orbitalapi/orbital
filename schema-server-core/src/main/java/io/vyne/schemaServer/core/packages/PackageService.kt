package io.vyne.schemaServer.core.packages

import io.vyne.PackageIdentifier
import io.vyne.ParsedPackage
import io.vyne.UriSafePackageIdentifier
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schema.publisher.PublisherType
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.packages.PackageWithDescription
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemaServer.packages.SourcePackageDescription
import io.vyne.schemas.DefaultPartialSchema
import io.vyne.schemas.PartialSchema
import io.vyne.spring.http.NotFoundException
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
                  val repositoryName = (packageDescription.packageConfig as GitRepositoryConfig).name
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

   @GetMapping("/api/packages")
   override fun listPackages(): Mono<List<SourcePackageDescription>> {
      val packages = schemaStore.schemaSet.parsedPackages.map { parsedPackage ->
         buildPackageDescription(parsedPackage)
      }
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
