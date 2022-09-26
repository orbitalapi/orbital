package io.vyne.queryService.changelog

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
