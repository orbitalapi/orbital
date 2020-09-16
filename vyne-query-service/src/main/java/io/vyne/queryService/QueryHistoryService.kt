package io.vyne.queryService

import io.vyne.query.Lineage
import io.vyne.query.ProfilerOperationDTO
import io.vyne.queryService.schemas.SchemaPreview
import io.vyne.queryService.schemas.SchemaPreviewRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import kotlin.streams.toList

@RestController
class QueryHistoryService(private val history: QueryHistory, private val queryHistoryExporter: QueryHistoryExporter, private val parsedDataExporter: ParsedDataExporter) {
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

   @GetMapping("/api/query/history/{id}/{type}/export")
   fun getQueryExport(@PathVariable("id") queryId: String, @PathVariable("type") exportType: ExportType): Mono<ByteArray> {
      return history.get(queryId).map {
         queryHistoryExporter.export(it.response.results, exportType)
      }
   }

   @GetMapping("/api/parsed/{type}/export")
   fun getParsedDataExport(@RequestBody request: Map<String, Any?>, @PathVariable("type") exportType: ExportType2): ByteArray {
      return parsedDataExporter.export(request, exportType)
   }
}


