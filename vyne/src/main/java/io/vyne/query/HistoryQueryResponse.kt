package io.vyne.query

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.schemas.QualifiedName
import java.util.*

data class HistoryQueryResponse(val results: Map<String, Any?> = mapOf(),
                                val unmatchedNodes: List<QualifiedName> = listOf(),
                                val fullyResolved: Boolean,
                                val queryResponseId: String = UUID.randomUUID().toString(),
                                val resultMode: ResultMode,
                                @field:JsonIgnore
                                val profilerOperation: ProfilerOperationDTO?,
                                val remoteCalls: List<RemoteCall> = listOf(),
                                val timings: Map<OperationType, Long> = mapOf(),
                                val error: String? = null) {
   // HACK : Put this last, so that other stuff is serialized first
   val lineageGraph = LineageGraph
}
