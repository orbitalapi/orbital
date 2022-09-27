package io.vyne.schemaServer.packages

import io.vyne.PackageIdentifier
import io.vyne.ParsedPackage
import io.vyne.UriSafePackageIdentifier
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schemas.DefaultPartialSchema
import io.vyne.schemas.PartialSchema
import io.vyne.spring.http.NotFoundException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


@RestController
class PackageService(
   private val expiringSourcesStore: ExpiringSourcesStore,
   private val schemaStore: SchemaStore
) : PackagesServiceApi {

   @GetMapping("/api/packages/{packageUri}")
   override fun loadPackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<ParsedPackage> {
      val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)
      val sourcePackage = schemaStore.schemaSet.parsedPackages.firstOrNull { it.identifier == packageIdentifier }
         ?: throw NotFoundException("Package $packageIdentifier was not found on this server")
      return Mono.just(sourcePackage)
   }

   @GetMapping("/api/packages")
   override fun listPackages(): Mono<List<SourcePackageDescription>> {
      val packages = schemaStore.schemaSet.parsedPackages.map { parsedPackage ->
         SourcePackageDescription(
            parsedPackage.identifier,
            expiringSourcesStore.getPublisherHealth(parsedPackage.identifier),
            parsedPackage.sources.size,
            0, // TODO : Warning count
            parsedPackage.sourcesWithErrors.size,
         )
      }
      return Mono.just(packages)
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
