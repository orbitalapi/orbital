package io.vyne.query

import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.Operation
import io.vyne.schemas.Service
import org.springframework.stereotype.Component

// Note:  Currently tested via tests in VyneTest, no direct tests, but that'd be good to add.
/**
 * Query strategy that will invoke services that return the requested type,
 * and do not require any parameters
 */
@Component
class DirectServiceInvocationStrategy(private var invokers: List<OperationInvoker>) : QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {

      return context.startChild(this, "look for candidate services", OperationType.LOOKUP) { profilerOperation ->
         val noArgOperationsByReturnType = context.schema.operations.filter {
            it.parameters.isEmpty()
         }.map { it.returnType to it }.toMap()

         val matchedNodes = target
            .filter { queryNode -> noArgOperationsByReturnType.containsKey(queryNode.type) }
            .map { queryNode -> noArgOperationsByReturnType[queryNode.type]!! to queryNode }
            .map { (operation, queryNode) ->
               val (service, _) = context.schema.operation(operation.qualifiedName)
               val result = invokeOperation(service, operation, queryNode, profilerOperation)
               queryNode to result
            }.toMap()

         QueryStrategyResult(matchedNodes)
      }

   }

   private fun invokeOperation(service: Service, operation: Operation, queryNode: QuerySpecTypeNode, profilerOperation: ProfilerOperation): TypedInstance {
      val invoker = invokers.firstOrNull { it.canSupport(service, operation) }
         ?: throw IllegalArgumentException("No invokers found for Operation ${operation.name}")

      return invoker.invoke(service, operation, emptyList(), profilerOperation)
   }
}
