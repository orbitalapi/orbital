package io.vyne.queryService

import io.vyne.models.TypedCollection
import io.vyne.query.*
import io.vyne.utils.log
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
class QueryHistoryService(private val history: QueryHistory) {
   private val truncationThreshold = 10

   @GetMapping("/api/query/history")
   fun listHistory(): Flux<QueryHistoryRecordUiWrapper> {
      return history.list().map {record -> QueryHistoryRecordUiWrapper(record, truncationThreshold) }
   }

   @GetMapping("/api/query/history/{id}/profile")
   fun getQueryProfile(@PathVariable("id") queryId: String): Mono<ProfilerOperationUIWrapper?> {
      return history.get(queryId).map { record -> record.response.profilerOperation?.let { ProfilerOperationUIWrapper(it, truncationThreshold)} }
   }

   // Both wrappers truncate vyne-results/service-responses so that 1000 records does not overload UI
   // Temporary solution until we introduce pagination

   class QueryHistoryRecordUiWrapper(record: QueryHistoryRecord<out Any>, private val truncationThreshold: Int) {
      val query: Any = record.query
      val response: HistoryQueryResponse = truncateResponse(record.response)
      val timestamp: Instant = record.timestamp
      val id: String = response.queryResponseId
      private fun truncateResponse(response: HistoryQueryResponse): HistoryQueryResponse {
         if (responseAboveThreshold(response, truncationThreshold)) {
            val results = response.results
               .map { (querySpecTypeNode, value) ->
                  val truncatedValue = when (value) {
                     is TypedCollection -> {
                        log().info("Truncating history record id={} response from {} to {}", response.queryResponseId, value.value.size, truncationThreshold)
                        TypedCollection.from(value.value.take(truncationThreshold))
                     }
                     else -> value
                  }
                  querySpecTypeNode to truncatedValue
               }.toMap()
            val truncated = true
            return HistoryQueryResponse(results,
               response.unmatchedNodes,
               response.path,
               response.queryResponseId,
               response.resultMode,
               response.profilerOperation,
               response.remoteCalls,
               response.timings,
               response.isFullyResolved,
               truncated)
         }
         return response
      }

      private fun responseAboveThreshold(response: HistoryQueryResponse, truncationThreshold: Int): Boolean {
         return (response.results).any { (querySpecTypeNode, value) ->
            when (value) {
               is TypedCollection -> value.value.size > truncationThreshold
               else -> false
            }
         }
      }
   }

   class ProfilerOperationUIWrapper(operation: ProfilerOperationDTO, private val truncationThreshold: Int = 10)  {
      val componentName: String = operation.componentName
      val operationName: String = operation.operationName
      val children: List<ProfilerOperationUIWrapper> = operation.children.map { ProfilerOperationUIWrapper(it) }
      val result: Result? = operation.result?.let { Result(it.startTime, it.endTime, truncateResult(it.value)) }// ui does not use that, consider setting to empty
      val type: OperationType = operation.type
      val duration: Long = operation.duration
      val context: MutableMap<String, Any?> = HashMap(operation.context)
      val remoteCalls: List<RemoteCall> = operation.remoteCalls.map {
         RemoteCall(it.service,
            it.addresss,
            it.operation,
            it.responseTypeName,
            it.method,
            it.requestBody,
            it.resultCode,
            it.durationMs,
            truncateRemoteCallResponse(it.response))
      }

      private fun truncateRemoteCallResponse(response: Any?) : Any? {
         return when(response) {
            is List<*> -> {
               return if (response.size > truncationThreshold) {
                  log().info("Truncating remote call response from {} to {}", response.size, truncationThreshold)
                  context["RemoteCallsTruncated"] = true
                  context["RemoteCallsOriginalSize"] = response.size
                  response.take(truncationThreshold)
               } else {
                  response
               }
            }
            else -> response
         }
      }

      private fun truncateResult(value: Any?): Any? {
         return when(value) {
            is TypedCollection -> {
               log().info("Truncating TypedCollection from {} to {}", value.type.fullyQualifiedName, value.value.size, truncationThreshold)
               return if (value.size > truncationThreshold) {
                  context["ResultTruncated"] = true
                  context["ResultOriginalSize"] = value.size
                  TypedCollection.from(value.value.take(truncationThreshold))
               } else {
                  value
               }
            }
            else -> value
         }
      }
   }

}
