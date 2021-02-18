package io.vyne.query

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.schemas.QualifiedName
import java.util.*

data class HistoryQueryResponse(val results: Map<String, Any?> = mapOf(),
                                val unmatchedNodes: List<QualifiedName> = listOf(),
                                val fullyResolved: Boolean,
                                override val queryResponseId: String = UUID.randomUUID().toString(),
                                @field:JsonIgnore
                                override val profilerOperation: ProfilerOperationDTO?,
                                override val responseStatus: QueryResponse.ResponseStatus,
                                override val remoteCalls: List<RemoteCall> = listOf(),
                                override val timings: Map<OperationType, Long> = mapOf(),
                                val error: String? = null):QueryResponse {
   val resultSize: Int = results.values.filterNotNull()
      .map { result ->
         when (result) {
            is Collection<*> -> result.size
            else -> 1
         }
      }.sum()

   override val message: String? = error

   // TODO ... how do we work this out?
   val durationMs = profilerOperation?.duration ?: 0;

   override val isFullyResolved: Boolean = fullyResolved
   override fun historyRecord(): HistoryQueryResponse = this
}
