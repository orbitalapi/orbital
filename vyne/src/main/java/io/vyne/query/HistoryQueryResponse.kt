package io.vyne.query

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.schemas.QualifiedName
import java.util.*

data class HistoryQueryResponse(val results: Map<String, Any?>? = mapOf(),
                                val unmatchedNodes: List<QualifiedName> = listOf(),
                                val fullyResolved: Boolean,
                                val queryResponseId: String = UUID.randomUUID().toString(),
                                @field:JsonIgnore
                                val profilerOperation: ProfilerOperationDTO?,
                                val responseStatus: QueryResponse.ResponseStatus,
                                val remoteCalls: List<RemoteCall> = listOf(),
                                val timings: Map<OperationType, Long> = mapOf(),
                                val error: String? = null) {
   val resultSize: Int?

   // TODO ... how do we work this out?
   val durationMs = profilerOperation?.duration ?: 0;
   init {
      resultSize = results?.values?.filterNotNull()
         ?.map { result ->
            when (result) {
               is Collection<*> -> result.size
               else -> 1
            }
         }?.sum()
   }
}
