package io.vyne.query

import com.google.common.annotations.VisibleForTesting
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.queryBuilders.QueryGrammarQueryBuilder
import io.vyne.query.queryBuilders.VyneQlGrammarQueryBuilder
import io.vyne.schemas.OutputConstraint
import io.vyne.schemas.Parameter
import io.vyne.schemas.PropertyToParameterConstraint
import io.vyne.schemas.QueryOperation
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.utils.log
import org.springframework.stereotype.Component

@Component
class QueryOperationInvocationStrategy(invocationService: OperationInvocationService,
                                       private val queryBuilders: List<QueryGrammarQueryBuilder> = listOf(VyneQlGrammarQueryBuilder())) : QueryStrategy, BaseOperationInvocationStrategy(invocationService) {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult {
      val candidateOperations = lookForCandidateQueryOperations(context, target)
      return invokeOperations(candidateOperations, context, target)
   }

   private fun lookForCandidateQueryOperations(context: QueryContext, target: Set<QuerySpecTypeNode>): Map<QuerySpecTypeNode, Map<RemoteOperation, Map<Parameter, TypedInstance>>> {
      return target.map { querySpecTypeNode ->
         querySpecTypeNode to lookForCandidateQueryOperations(context.schema, querySpecTypeNode)
      }.toMap()
   }

   @VisibleForTesting
   internal fun lookForCandidateQueryOperations(schema: Schema, target: QuerySpecTypeNode): Map<RemoteOperation, Map<Parameter, TypedInstance>> {
      val result = schema.services
         .flatMap { it.queryOperations }
         .asSequence()
         .filter { it.returnType == target.type }
         .filter { it.hasFilterCapability }
         .filter { queryServiceSatisfiesConstraints(it, target.dataConstraints) }
         .mapNotNull { queryOperation ->
            val grammarBuilder = this.queryBuilders.firstOrNull { it.canSupport(queryOperation.grammar) };
            if (grammarBuilder == null) {
               log().warn("No support found for grammar ${queryOperation.grammar}, so will be excluded from query plan")
               null
            } else {
               queryOperation to grammarBuilder
            }
         }
         .map { (queryOperation, grammarBuilder) ->
            (queryOperation as RemoteOperation) to grammarBuilder.buildQuery(target, queryOperation)
         }
         .toList().toMap()
      return result
   }

   private fun queryServiceSatisfiesConstraints(queryOperation: QueryOperation, dataConstraints: List<OutputConstraint>): Boolean {
      // bail early
      if (dataConstraints.isEmpty()) {
         return true
      }

      // For now, we're only looking at filter operations.  Revisit when we get to aggregations.
      return dataConstraints.all { constraint ->
         when (constraint) {
            is PropertyToParameterConstraint -> queryOperation.supportedFilterOperations.contains(constraint.operator)
            else -> {
               // TODO : Implement support for the other constraints if/when they become
               log().warn("Support for data constraint of type ${constraint::class.simpleName} is not yet implemented, so query operations cannot be invoked for this query.")
               false
            }
         }
      }
   }


}
