package io.vyne.cockpit.core.changelog

import io.vyne.UriSafePackageIdentifier
import io.vyne.schemaServer.changelog.ChangeLogEntry
import io.vyne.schemaServer.changelog.ChangelogApi
import io.vyne.spring.config.ExcludeFromOrbitalStation
import io.vyne.spring.http.handleFeignErrors
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

// Simple pass-through to the Schema server
@RestController
@ExcludeFromOrbitalStation
class ChangeLogServiceFacade(private val changeLogApi: ChangelogApi) {

   @GetMapping("/api/changelog")
   fun getChangelog(): Mono<List<ChangeLogEntry>> = handleFeignErrors {
      changeLogApi.getChangelog()
   }

   @GetMapping("/api/changelog/{packageName}")
   fun getChangelog(@PathVariable("packageName") packageName: UriSafePackageIdentifier): Mono<List<ChangeLogEntry>> =
      handleFeignErrors {
         changeLogApi.getChangelog(packageName)
      }

}
