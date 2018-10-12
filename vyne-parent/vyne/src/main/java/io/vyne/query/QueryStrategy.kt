package io.vyne.query

import io.vyne.models.TypedInstance

data class QueryStrategyResult(
   val matchedNodes: Map<QuerySpecTypeNode, TypedInstance?> = emptyMap(),
   val additionalData: Set<TypedInstance> = emptySet()
) {
   companion object {
       fun empty():QueryStrategyResult = QueryStrategyResult()
   }
}

interface QueryStrategy {
   fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult
}
