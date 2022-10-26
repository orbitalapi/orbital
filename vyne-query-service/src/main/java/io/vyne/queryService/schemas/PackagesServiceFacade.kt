package io.vyne.queryService.schemas

import io.orbital.station.IncludeInVyneOnly
import io.vyne.ParsedPackage
import io.vyne.UriSafePackageIdentifier
import io.vyne.queryService.utils.handleFeignErrors
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemaServer.packages.SourcePackageDescription
import io.vyne.schemas.PartialSchema
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


// Simple pass-through to the Schema server
@RestController
// When running as orbital, the
@IncludeInVyneOnly
class PackagesServiceFacade(private val packagesServiceApi: PackagesServiceApi) {

   @GetMapping("/api/packages")
   fun listPackages(): Mono<List<SourcePackageDescription>> = handleFeignErrors {
      packagesServiceApi.listPackages()
   }

   @GetMapping("/api/packages/{packageUri}")
   fun loadPackage(@PathVariable("packageUri") packageUri: String): Mono<ParsedPackage> = handleFeignErrors {
      packagesServiceApi.loadPackage(packageUri)
   }


   @GetMapping("/api/packages/{packageUri}/schema")
   fun getPartialSchemaForPackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<PartialSchema> =
      handleFeignErrors {
         packagesServiceApi.getPartialSchemaForPackage(packageUri)
      }
}

