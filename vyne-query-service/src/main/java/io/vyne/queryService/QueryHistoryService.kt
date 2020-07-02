package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.models.DataSource
import io.vyne.models.TypedCollection
import io.vyne.query.*
import io.vyne.utils.log
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import kotlin.collections.HashMap
import kotlin.streams.toList

@RestController
class QueryHistoryService(private val history: QueryHistory) {
   private val truncationThreshold = 10

   @GetMapping("/api/query/history")
   fun listHistory(): String {
      val queries = history.list()
         .toStream().toList()
      val json = Lineage.newLineageAwareJsonMapper()
         .writeValueAsString(queries)
      return json
   }

   @GetMapping("/api/query/history/{id}/profile")
   fun getQueryProfile(@PathVariable("id") queryId: String): Mono<ProfilerOperationDTO?> {
      return history.get(queryId).map { it.response.profilerOperation }
   }

}
