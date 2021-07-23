package io.vyne.query

import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

data class QueryStrategyResult(
   // This needs to be nullable at the moment because we use the
   // absence to signal that the flow will never emit any matched values.
   // We need to revisit this, and find a cleaner way.
   // Have named nullableMatchedNodes so that the nullability isn't leaked
   // outside of the class.
   private val nullableMatchedNodes: Flow<TypedInstance>?,
   val failedAttempts:List<DataSource> = emptyList()
) {
   val matchedNodes: Flow<TypedInstance>
      get() {
         return this.nullableMatchedNodes ?: emptyFlow()
      }


   //TODO - determine if the matchednodes flow will return any elements - a crude null check antipattern right now
   fun hasMatchesNodes(): Boolean {
      return nullableMatchedNodes != null
   }

   /**
    * Consumes the flow (destructively, in a blocking call).
    * Really, only useful for debugging.
    */
   fun consumeFlow():List<TypedInstance> {
      return runBlocking { matchedNodes.toList() }
   }

   companion object {
      fun searchFailed(failedAttempts: List<DataSource> = emptyList()): QueryStrategyResult = QueryStrategyResult(null, failedAttempts)
      fun from(value:TypedInstance, failedAttempts: List<DataSource> = emptyList()) = QueryStrategyResult(flowOf(value), failedAttempts)
   }
}

interface QueryStrategy {
   suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult
}
