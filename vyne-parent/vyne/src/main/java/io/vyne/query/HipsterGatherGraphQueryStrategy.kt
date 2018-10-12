package io.vyne.query

import io.osmosis.polymer.models.TypedCollection
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.operation
import io.osmosis.polymer.utils.log
import io.vyne.query.QueryMode

class HipsterGatherGraphQueryStrategy(private val graphQueryStrategy: HipsterDiscoverGraphQueryStrategy) : QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      val targetType = target.first().type
      val queryResults = target.filter { it.mode == QueryMode.GATHER }
         .flatMap { querySpec ->
            //            querySpec to
            context.schema.operationsWithReturnType(querySpec.type).map { (service, operation) -> Triple(querySpec, service, operation) }
         }.flatMap { (originalQuerySpec, service, operation) ->
            val operationElement = operation(service, operation)
            log().debug("Gather strategy deferring to discover from operation ${operationElement.valueAsQualifiedName()} ")
            val result = context.find(QuerySpecTypeNode(operation.returnType), context.facts)

            // Convert the "QuerySpecTypeNode" in the result to the one that was originally requested
            result.results.map { (spec, result) -> originalQuerySpec to result }
            // Convert List<Pair<QuerySpecTypeNode,TypedInstance?>> -> Map<QuerySpecTypeNode,List<TypedInstance?>>
         }.groupBy({ it.first }, { it.second })
         .map { (querySpec: QuerySpecTypeNode, results: List<TypedInstance?>) ->
            // Collate the independent TypedInstance into a TypedCollection
            // So the result will be: (spec: Type<T>, value: TypedCollection<T>)
            // I chose to do this so that there's still a single return value for each
            // querySpecTypeNode in the result.
            // The alternative is to use a multi-map, which is a pretty annoying change to the API.
            querySpec to TypedCollection(querySpec.type, results.filterNotNull())
         }.toMap()
      return QueryStrategyResult(queryResults)
   }

}
