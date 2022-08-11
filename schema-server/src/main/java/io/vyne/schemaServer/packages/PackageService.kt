package io.vyne.schemaServer.packages

import io.vyne.PackageIdentifier
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schema.publisher.PublisherHealth
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class SourcePackageDescription(
   val identifier: PackageIdentifier,
   val health: PublisherHealth,
   val sourceCount: Int,
   val warningCount: Int,
   val errorCount: Int,
   // TODO : Other things for visualisation
) {
   val uriPath: String = identifier.toString().replace("/", ":")
}

@RestController
class PackageService(
   private val expiringSourcesStore: ExpiringSourcesStore,
   private val validatingStore: ValidatingSchemaStoreClient
) {

   @GetMapping("/api/packages")
   fun listPackages(): List<SourcePackageDescription> {
      return validatingStore.schemaSet.parsedPackages.map { parsedPackage ->
         SourcePackageDescription(
            parsedPackage.identifier,
            expiringSourcesStore.getPublisherHealth(parsedPackage.identifier),
            parsedPackage.sources.size,
            0, // TODO : Warning count
            parsedPackage.sourcesWithErrors.size,
         )
      }
   }
}
