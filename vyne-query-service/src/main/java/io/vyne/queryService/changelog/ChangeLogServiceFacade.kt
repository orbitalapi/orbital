package io.vyne.queryService.changelog

import io.orbital.station.IncludeInVyneOnly
import io.vyne.UriSafePackageIdentifier
import io.vyne.queryService.utils.handleFeignErrors
import io.vyne.schemaServer.changelog.ChangeLogEntry
import io.vyne.schemaServer.changelog.ChangelogApi
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

// Simple pass-through to the Schema server
@RestController
// Only active in Vyne, because in Orbital we have the actual changelog API
// from the Schema server
@IncludeInVyneOnly
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
