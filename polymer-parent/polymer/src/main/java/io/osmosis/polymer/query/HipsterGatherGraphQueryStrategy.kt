package io.osmosis.polymer.query

import io.osmosis.polymer.operation
import io.osmosis.polymer.query.graph.EdgeEvaluator
import io.osmosis.polymer.type
import io.osmosis.polymer.utils.log

class HipsterGatherGraphQueryStrategy(private val graphQueryStrategy: HipsterDiscoverGraphQueryStrategy) : QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      val targetType = target.first().type
      val results = target.filter { it.mode == QueryMode.GATHER }
         .flatMap { querySpec ->
            context.schema.operationsWithReturnType(querySpec.type)
         }.map { (service, operation) ->
            val operationElement = operation(service, operation)
            // TODO : I think this is wrong, all we're gonna discover is a path to the
            // operation, not actually invoke it.
            log().debug("Gather strategy deferring to discover from operation ${operationElement.valueAsQualifiedName()} ")
            // HACK for exploration .. should be looking at the operation
            val result = graphQueryStrategy.find(type(targetType), context)
            result
         }
      // Still a WIP.
      return QueryStrategyResult.empty()
   }

}
