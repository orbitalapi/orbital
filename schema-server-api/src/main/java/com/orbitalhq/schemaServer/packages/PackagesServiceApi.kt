package com.orbitalhq.schemaServer.packages

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.ParsedPackage
import com.orbitalhq.UriSafePackageIdentifier
import com.orbitalhq.schema.publisher.PublisherHealth
import com.orbitalhq.schema.publisher.PublisherType
import com.orbitalhq.schemas.PartialSchema
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

data class SourcePackageDescription(
   val identifier: PackageIdentifier,
   val health: PublisherHealth,
   val sourceCount: Int,
   val warningCount: Int,
   val errorCount: Int,
   val publisherType: PublisherType,
   val editable: Boolean,
   val packageConfig: Any? // GitRepositoryConfig or  FileSystemPackageSpec
   // TODO : Other things for visualisation
) {
   val uriPath: String = PackageIdentifier.toUriSafeId(identifier)

   companion object {
   }

}


// Please note that feign client name is specified through vyne.taxonomyServer.name
// We can use vyne.schema-server.name here as there is another @ReactiveFeignClient - SchemaEditorApi
// which uses vyne.schema-server.name configuration option. Due to a bug in playtika, qualifier is not considered properly
// to distinguish these definitions and hence we need to use a different configuration setting here.
@ReactiveFeignClient("\${vyne.package-server.name:schema-server}", qualifier = "schemaPackagesFeignClient")
interface PackagesServiceApi {

   @GetMapping("/api/packages")
   fun listPackages(): Mono<List<SourcePackageDescription>>

   @GetMapping("/api/packages/{packageUri}")
   fun loadPackage(@PathVariable("packageUri") packageUri: String): Mono<PackageWithDescription>

   @GetMapping("/api/packages/{packageUri}/schema")
   fun getPartialSchemaForPackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<PartialSchema>

   @DeleteMapping("/api/packages/{packageUri}")
   fun removePackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<Unit>
}

data class PackageWithDescription(
   val parsedPackage: ParsedPackage,
   val description: SourcePackageDescription
) {
   companion object {
      // for testing
      fun empty(packageIdentifier: PackageIdentifier): PackageWithDescription {
         return PackageWithDescription(
            ParsedPackage(
               PackageMetadata.from(packageIdentifier),
               emptyList(),
               emptyMap()
            ),
            SourcePackageDescription(
               packageIdentifier,
               PublisherHealth(PublisherHealth.Status.Healthy),
               0,
               0,
               0,
               PublisherType.FileSystem,
               true,
               null
            )
         )

      }
   }
}
