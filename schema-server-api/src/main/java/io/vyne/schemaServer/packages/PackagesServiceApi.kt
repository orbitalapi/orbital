package io.vyne.schemaServer.packages

import io.vyne.PackageIdentifier
import io.vyne.ParsedPackage
import io.vyne.UriSafePackageIdentifier
import io.vyne.schema.publisher.PublisherHealth
import io.vyne.schema.publisher.PublisherType
import io.vyne.schemas.DefaultPartialSchema
import io.vyne.schemas.PartialSchema
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
   val editable: Boolean
   // TODO : Other things for visualisation
) {
   val uriPath: String = PackageIdentifier.toUriSafeId(identifier)
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
   fun loadPackage(@PathVariable("packageUri") packageUri: String): Mono<ParsedPackage>

   @GetMapping("/api/packages/{packageUri}/schema")
   fun getPartialSchemaForPackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<PartialSchema>
}
