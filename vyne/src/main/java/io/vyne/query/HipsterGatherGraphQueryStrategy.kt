package io.vyne.query

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operation
import io.vyne.utils.log

/***
 * This strategy is not being used anymore (see QueryEngineFactory withOperationInvokers method that is instantiating DefaultQueryEngineFactory
 * without an instance of this class in its list of Query strategies.). There are two reason for this:
 * 1. This strategy does not take 'dataConstraints' into account for the given 'QuerySpecTypeNode')
 * 2. Strategies derived from 'BaseOperationInvocationStrategy' provides the correct 'GATHER' behaviour by taking into 'data constraints' and hence making
 * this class obselete.
 *
 * Here is a concrete example illustrating the issues with this class:
 * given the the following schema:
 *
 *  namespace Bar {
 *        type Isin inherits String
 *        model Order {
 *           isin: Isin
 *        }
 *
 *        service OrderService {
 *           operation findAll(): Order[]
 *           operation findOrder(): Order
 *           }
 *        }
 *      }
 *
 * Execution of:
 *   findAll { Bar.Order[](Isin= 'IT0000312312')}
 *
 * Should not invoke 'findAll' or 'findOrder' operations due to 'Isin= 'IT0000312312'
 * strategies derived from BaseOperationInvocationStrategy do exactly that and they don't invoke these operations.
 * However, HipsterGatherGraphQueryStrategy ignore ('Isin= 'IT0000312312') constraint, invokes 'findAll' and returns an 'invalid'
 * successful QueryStrategyResult.
 *
 */
class HipsterGatherGraphQueryStrategy() : QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult {
      val targetType = target.first().type
      val spec = invocationConstraints.typedInstanceValidPredicate
      val queryResults = target.filter { it.mode == QueryMode.GATHER }
         .flatMap { querySpec ->
            //            querySpec to
            context.schema.operationsWithReturnType(querySpec.type).map { (service, operation) -> Triple(querySpec, service, operation) }
         }.flatMap { (originalQuerySpec, service, operation) ->
            val operationElement = operation(service, operation)
            log().debug("Gather strategy deferring to discover from operation {} ", lazy { operationElement.valueAsQualifiedName() })
            val result = context.find(QuerySpecTypeNode(operation.returnType))

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
            querySpec to TypedCollection.arrayOf(querySpec.type, results.filterNotNull())
         }.toMap()
      return QueryStrategyResult(queryResults)
   }

}
