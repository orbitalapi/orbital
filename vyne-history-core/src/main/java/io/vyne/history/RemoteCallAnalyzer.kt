package io.vyne.history

import io.vyne.query.RemoteCall
import io.vyne.query.RemoteOperationPerformanceStats
import io.vyne.query.ResponseCodeGroup
import io.vyne.query.ResponseMessageType
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import java.math.RoundingMode

class RemoteCallAnalyzer {
   fun generateStats(remoteCalls: List<RemoteCall>): List<RemoteOperationPerformanceStats> {
      return remoteCalls.groupBy { it.operationQualifiedName }
         .map { (name, calls) -> generateCallStats(name, calls) }
   }

   private fun generateCallStats(name: QualifiedName, calls: List<RemoteCall>): RemoteOperationPerformanceStats {
      // Calls initiated is not the same as the number of calls,
      // as when streaming, a request has many responses
      val callsInitiated = calls.map { it.remoteCallId }.distinct()
         .size

      val callsByRequestId = calls.groupBy { it.remoteCallId }
      val averageTimeToFirstResponse = callsByRequestId.mapNotNull { (requestId, responses) ->
         responses.minByOrNull { it.durationMs }?.durationMs
      }.average().toBigDecimal().setScale(2, RoundingMode.HALF_DOWN)

      // we only consider the responses from
      // FULL responses, as other responses are streaming, and not reliable indication
      // of performance of the remote server
      val fullResponseMessages = calls.filter { it.responseMessageType == ResponseMessageType.FULL }
      val totalServiceWaitTime = if (fullResponseMessages.isNotEmpty()) {
         fullResponseMessages
            .sumBy { it.durationMs.toInt() }
      } else null

      val responseCodeGroups = calls.groupBy { ResponseCodeGroup.groupFromCode(it.resultCode) }
      val responseCodeTable = ResponseCodeGroup.values().associate { responseCodeGroup ->
         responseCodeGroup to (responseCodeGroups[responseCodeGroup]?.size ?: 0)
      }

      val (serviceName, operationName) = OperationNames.serviceAndOperation(name)
      return RemoteOperationPerformanceStats(
         name,
         serviceName,
         operationName,
         callsInitiated,
         averageTimeToFirstResponse,
         totalServiceWaitTime,
         responseCodeTable
      )
   }
}


