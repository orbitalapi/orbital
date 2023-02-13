package io.vyne.history

import io.vyne.query.HttpExchange
import io.vyne.query.RemoteOperationPerformanceStats
import io.vyne.query.ResponseCodeGroup
import io.vyne.query.ResponseMessageType
import io.vyne.query.history.BasePartialRemoteCallResponse
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import java.math.RoundingMode

class RemoteCallAnalyzer {
   fun generateStats(remoteCalls: List<BasePartialRemoteCallResponse>): List<RemoteOperationPerformanceStats> {
      return remoteCalls.groupBy { it.operation }
         .map { (name, calls) -> generateCallStats(name, calls) }
   }

   private fun generateCallStats(
      name: QualifiedName,
      calls: List<BasePartialRemoteCallResponse>
   ): RemoteOperationPerformanceStats {
      // Calls initiated is not the same as the number of calls,
      // as when streaming, a request has many responses
      val callsInitiated = calls.map { it.remoteCallId }.distinct()
         .size

      val callsByRequestId = calls.groupBy { it.remoteCallId }
      val averageTimeToFirstResponse = callsByRequestId.mapNotNull { (requestId, responses) ->
         responses
            .filter { it.durationMs != null }
            .minByOrNull { it.durationMs!! }?.durationMs
      }.average().toBigDecimal().setScale(2, RoundingMode.HALF_DOWN)

      // we only consider the responses from
      // FULL responses, as other responses are streaming, and not reliable indication
      // of performance of the remote server
      val fullResponseMessages = calls.filter { it.messageKind == ResponseMessageType.FULL }
      val totalServiceWaitTime = if (fullResponseMessages.isNotEmpty()) {
         fullResponseMessages
            .sumBy { it.durationMs?.toInt() ?: 0 }
      } else null

      val responseCodeGroups = calls
         .groupBy {
            when (it.exchange) {
               is HttpExchange -> ResponseCodeGroup.groupFromCode((it.exchange as HttpExchange).responseCode)
               else -> ResponseCodeGroup.fromSuccess(it.success)
            }
         }
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


