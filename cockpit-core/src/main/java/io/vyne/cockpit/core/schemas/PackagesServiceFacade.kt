package io.vyne.cockpit.core.schemas

import io.vyne.ParsedPackage
import io.vyne.UriSafePackageIdentifier
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemaServer.packages.SourcePackageDescription
import io.vyne.schemas.PartialSchema
import io.vyne.spring.config.ExcludeFromOrbitalStation
import io.vyne.spring.http.handleFeignErrors
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


// Simple pass-through to the Schema server
@RestController
@ExcludeFromOrbitalStation
// When running as orbital, the
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

