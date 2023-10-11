package com.orbitalhq.cockpit.core.schemas

import com.orbitalhq.UriSafePackageIdentifier
import com.orbitalhq.schemaServer.packages.PackageWithDescription
import com.orbitalhq.schemaServer.packages.PackagesServiceApi
import com.orbitalhq.schemaServer.packages.SourcePackageDescription
import com.orbitalhq.schemas.PartialSchema
import com.orbitalhq.spring.config.ExcludeFromOrbitalStation
import com.orbitalhq.spring.http.handleFeignErrors
import org.springframework.web.bind.annotation.DeleteMapping
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
   fun loadPackage(@PathVariable("packageUri") packageUri: String): Mono<PackageWithDescription> = handleFeignErrors {
      packagesServiceApi.loadPackage(packageUri)
   }


   @GetMapping("/api/packages/{packageUri}/schema")
   fun getPartialSchemaForPackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<PartialSchema> =
      handleFeignErrors {
         packagesServiceApi.getPartialSchemaForPackage(packageUri)
      }

   @DeleteMapping("/api/packages/{packageUri}")
   fun removePackage(@PathVariable("packageUri") packageUri: UriSafePackageIdentifier): Mono<Unit> = handleFeignErrors {
      packagesServiceApi.removePackage(packageUri)
   }

}
