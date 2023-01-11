package io.vyne.query

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

// Note : Also models failures, so is fairly generic
interface QueryResponse {
   @Serializable
   enum class ResponseStatus {
      UNKNOWN,
      COMPLETED,
      RUNNING,

      // Ie., the query didn't error, but not everything was resolved
      INCOMPLETE,
      ERROR,
      CANCELLED
   }

   val responseStatus: ResponseStatus
   val queryResponseId: String
   val clientQueryId: String?
   val queryId: String

   @get:JsonProperty("fullyResolved")
   val isFullyResolved: Boolean
   val profilerOperation: ProfilerOperation?
   val remoteCalls: List<RemoteCall>
      get() = collateRemoteCalls(this.profilerOperation)

   val timings: Map<OperationType, Long>
      get() {
         return profilerOperation?.timings ?: emptyMap()
      }

   val vyneCost: Long
      get() = profilerOperation?.vyneCost ?: 0L

   val responseType: String?

}
