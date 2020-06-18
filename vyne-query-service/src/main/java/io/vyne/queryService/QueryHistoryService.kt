package io.vyne.queryService

import io.vyne.query.ProfilerOperationDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class QueryHistoryService(val history: QueryHistory) {

   @GetMapping("/query/history")
   fun listHistory(): Flux<QueryHistoryRecord<out Any>> {
      return history.list()
   }

   @GetMapping("/query/history/{id}/profile")
   fun getQueryProfile(@PathVariable("id") queryId: String): Mono<ProfilerOperationDTO?> {
      return history.get(queryId).map { it.response.profilerOperation }
   }
}
