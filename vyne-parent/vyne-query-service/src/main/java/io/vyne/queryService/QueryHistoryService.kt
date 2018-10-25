package io.vyne.queryService

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryHistoryService(val history: QueryHistory) {

   @GetMapping("/query/history")
   fun listHistory(): List<QueryHistoryRecord> {
      return history.list()
   }

}
