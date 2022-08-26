package io.vyne.schemaServer.packages

import io.vyne.PackageIdentifier
import io.vyne.ParsedPackage
import io.vyne.SourcePackage
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schema.publisher.PublisherHealth
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import io.vyne.spring.http.NotFoundException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


@RestController
class PackageService(
   private val expiringSourcesStore: ExpiringSourcesStore,
   private val validatingStore: ValidatingSchemaStoreClient
) : PackagesServiceApi {

   @GetMapping("/api/packages/{packageUri}")
   override fun loadPackage(@PathVariable("packageUri") packageUri: String): Mono<ParsedPackage> {
      val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)
      val sourcePackage = validatingStore.schemaSet.parsedPackages.firstOrNull { it.identifier == packageIdentifier }
         ?: throw NotFoundException("Package $packageIdentifier was not found on this server")
      return Mono.just(sourcePackage)
   }

   @GetMapping("/api/packages")
   override fun listPackages(): Mono<List<SourcePackageDescription>> {
      val packages = validatingStore.schemaSet.parsedPackages.map { parsedPackage ->
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
}
