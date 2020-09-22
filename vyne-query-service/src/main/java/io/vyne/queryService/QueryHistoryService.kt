package io.vyne.queryService

import io.vyne.query.Lineage
import io.vyne.query.ProfilerOperationDTO
import org.springframework.web.bind.annotation.*
import io.vyne.query.RemoteCall
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import kotlin.streams.toList

@RestController
class QueryHistoryService(private val history: QueryHistory, private val queryHistoryExporter: QueryHistoryExporter) {
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

   @GetMapping("/api/query/history/remotecalls/{id}/export")
   fun getRemoteCallExport(@PathVariable("id") queryId: String): ByteArray {
      val queries = history.list()
         .toStream().toList()
      var queryRemoteCall: List<RemoteCall> = listOf();
      queries.forEach { query -> if(query.id == queryId){
         queryRemoteCall = query.response.remoteCalls
      } }
      val json = Lineage.newLineageAwareJsonMapper()
         .writeValueAsString(queryRemoteCall)
      return json.toByteArray()
      }



   @GetMapping("/api/query/history/{id}/{type}/export")
   fun getQueryExport(@PathVariable("id") queryId: String, @PathVariable("type") exportType: ExportType): Mono<ByteArray> {
      return history.get(queryId).map {
         queryHistoryExporter.export(it.response.results, exportType)
      }
   }
}


