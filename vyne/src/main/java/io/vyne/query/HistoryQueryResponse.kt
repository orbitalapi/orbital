package io.vyne.query

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.schemas.QualifiedName
import java.util.*

data class HistoryQueryResponse(val results: Map<String, Any?>,
                                val unmatchedNodes: List<QualifiedName>,
                                val fullyResolved: Boolean,
                                val queryResponseId: String = UUID.randomUUID().toString(),
                                val resultMode: ResultMode,
                                @field:JsonIgnore
                                val profilerOperation: ProfilerOperationDTO?,
                                val remoteCalls: List<RemoteCall>,
                                val timings: Map<OperationType, Long>) {
   // HACK : Put this last, so that other stuff is serialized first
   val lineageGraph = LineageGraph
}
