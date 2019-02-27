package io.vyne.queryService

import io.vyne.query.ProfilerOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryHistoryService(val history: QueryHistory) {

   @GetMapping("/query/history")
   fun listHistory(): List<QueryHistoryRecord> {
      return history.list()
   }

   @GetMapping("/query/history/{id}/profile")
   fun getQueryProfile(@PathVariable("id") queryId: String): ProfilerOperation? {
      return history.get(queryId).response.profilerOperation
   }
}
