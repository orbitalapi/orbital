package io.vyne.query

import io.vyne.models.TypedInstance
import kotlinx.coroutines.flow.Flow

data class QueryStrategyResult(
   val matchedNodes: Flow<TypedInstance>? = null
) {

   //TODO - determine if the matchednodes flow will return any elements - a crude null check antipattern right now
   fun hasMatchesNodes(): Boolean {
      return matchedNodes != null
   }

   companion object {
       fun empty():QueryStrategyResult = QueryStrategyResult()
   }
}

interface QueryStrategy {
   suspend fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult
}
