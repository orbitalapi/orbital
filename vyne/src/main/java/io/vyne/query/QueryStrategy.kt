package io.vyne.query

import io.vyne.models.TypedInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class QueryStrategyResult(
   // This needs to be nullable at the moment because we use the
   // absence to signal that the flow will never emit any matched values.
   // We need to revisit this, and find a cleaner way.
   // Have named nullableMatchedNodes so that the nullability isn't leaked
   // outside of the class.
   private val nullableMatchedNodes: Flow<TypedInstance>? = null
) {
   val matchedNodes: Flow<TypedInstance>
      get() {
         return this.nullableMatchedNodes ?: emptyFlow()
      }


   //TODO - determine if the matchednodes flow will return any elements - a crude null check antipattern right now
   fun hasMatchesNodes(): Boolean {
      return nullableMatchedNodes != null
   }

   companion object {
      fun empty(): QueryStrategyResult = QueryStrategyResult()
   }
}

interface QueryStrategy {
   suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult
}
