package io.osmosis.polymer.query

import io.osmosis.polymer.models.TypedInstance

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
