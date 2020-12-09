package io.vyne.queryService

import io.vyne.ExecutableQuery
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RunningQueriesService(
   private val activeQueryRepository: ExecutingQueryRepository
) {
   @GetMapping("/api/query/active")
   fun listActiveQueries(): List<ExecutableQuery> {
      return activeQueryRepository.list()
   }
}
