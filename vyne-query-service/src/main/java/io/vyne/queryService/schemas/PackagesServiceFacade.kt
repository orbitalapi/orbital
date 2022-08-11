package io.vyne.queryService.schemas

import io.vyne.ParsedPackage
import io.vyne.queryService.utils.handleFeignErrors
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemaServer.packages.SourcePackageDescription
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


// Simple pass-through to the Schema server
@RestController
class PackagesServiceFacade(private val packagesServiceApi: PackagesServiceApi)  {

   @GetMapping("/api/schema/packages")
   fun listPackages(): Mono<List<SourcePackageDescription>> = handleFeignErrors {
      packagesServiceApi.listPackages()
   }

   @GetMapping("/api/schema/packages/{packageUri}")
   fun loadPackage(@PathVariable("packageUri") packageUri: String): Mono<ParsedPackage> = handleFeignErrors {
      packagesServiceApi.loadPackage(packageUri)
   }

}

