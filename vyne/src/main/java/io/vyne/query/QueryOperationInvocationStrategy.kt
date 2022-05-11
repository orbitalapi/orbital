package io.vyne.query

import com.google.common.annotations.VisibleForTesting
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.queryBuilders.QueryGrammarQueryBuilder
import io.vyne.query.queryBuilders.VyneQlGrammarQueryBuilder
import io.vyne.schemas.*
import io.vyne.utils.log

class QueryOperationInvocationStrategy(
   invocationService: OperationInvocationService,
   private val queryBuilders: List<QueryGrammarQueryBuilder> = listOf(VyneQlGrammarQueryBuilder())
) : QueryStrategy, BaseOperationInvocationStrategy(invocationService) {

   override suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      val candidateOperations = lookForCandidateQueryOperations(context, target)
      if (candidateOperations.values.all { it.isEmpty() }) {
         return QueryStrategyResult.searchFailed()
      }
      val result = invokeOperations(candidateOperations, context, target)
      return result
   }

   private fun lookForCandidateQueryOperations(
      context: QueryContext,
      target: Set<QuerySpecTypeNode>
   ): Map<QuerySpecTypeNode, Map<RemoteOperation, Map<Parameter, TypedInstance>>> {
      return target.associateWith { querySpecTypeNode ->
         lookForCandidateQueryOperations(context.schema, querySpecTypeNode)
      }
   }

   @VisibleForTesting
   internal fun lookForCandidateQueryOperations(
      schema: Schema,
      target: QuerySpecTypeNode
   ): Map<RemoteOperation, Map<Parameter, TypedInstance>> {
      val queryOperations = schema.services
         .flatMap { it.queryOperations }
         .filter { it.returnType.isAssignableTo(target.type) }
         .filter { it.hasFilterCapability }

      return queryOperations.filter {
         queryServiceSatisfiesConstraints(
            schema,
            it,
            target.dataConstraints,
            isCovariance(it.returnType, target.type)
         )
      }
         .mapNotNull { queryOperation -> findGrammarBuilder(queryOperation) }
         .map { (queryOperation, grammarBuilder) ->
            val queryTarget = if (isCovariance(queryOperation.returnType, target.type)) {
               target.copy(type = queryOperation.returnType)
            } else target

            queryOperation to grammarBuilder.buildQuery(queryTarget, queryOperation, schema)

         }
         .toList().toMap()
   }

   // TODO : We shouldn't be doing this kind of covariance checking here - these filters
   // belong in the type system.  We already have a.isAssignableTo(b) and a.isAssignableFrom(b),
   // why aren'te we using them here?
   private fun isCovariance(operationReturnType: Type, targetType: Type): Boolean {
      return operationReturnType.isCollection && targetType.isCollection &&
         operationReturnType.typeParameters[0].inheritsFrom(targetType.typeParameters[0])
   }

   private fun findGrammarBuilder(queryOperation: QueryOperation): Pair<QueryOperation, QueryGrammarQueryBuilder>? {
      val grammarBuilder = this.queryBuilders.firstOrNull { it.canSupport(queryOperation.grammar) };
      return if (grammarBuilder == null) {
         log().warn("No support found for grammar ${queryOperation.grammar}, so will be excluded from query plan")
         null
      } else {
         queryOperation to grammarBuilder
      }
   }

   private fun queryServiceSatisfiesConstraints(
      schema: Schema,
      queryOperation: QueryOperation,
      dataConstraints: List<OutputConstraint>,
      isCovariant: Boolean
   ): Boolean {
      // bail early
      if (dataConstraints.isEmpty()) {
         return true
      }

      // For now, we're only looking at filter operations.  Revisit when we get to aggregations.
      return dataConstraints.all { constraint ->
         when (constraint) {
            is PropertyToParameterConstraint -> if (isCovariant) {
               queryOperation.supportedFilterOperations.contains(constraint.operator)
                  && validateSupportedFilterOperations(schema, constraint, queryOperation.returnType)
            } else {
               queryOperation.supportedFilterOperations.contains(constraint.operator)
            }
            else -> {
               // TODO : Implement support for the other constraints if/when they become
               log().warn("Support for data constraint of type ${constraint::class.simpleName} is not yet implemented, so query operations cannot be invoked for this query.")
               false
            }
         }
      }
   }

   fun validateSupportedFilterOperations(
      schema: Schema,
      propertyToParameterConstraint: PropertyToParameterConstraint,
      operationReturnType: Type
   ): Boolean {
      val propertyConstraintTaxiType = propertyToParameterConstraint.propertyIdentifier.taxi
      val propertyConstraintVyneType = schema.type(propertyConstraintTaxiType)
      val operationReturnParameterisedType =
         if (operationReturnType.isCollection) operationReturnType.typeParameters[0] else operationReturnType
      return operationReturnParameterisedType.attributes.values.any { field ->
         val fieldVyneType = schema.type(field.type)
         fieldVyneType == propertyConstraintVyneType || propertyConstraintVyneType.inheritsFrom(fieldVyneType)
      }
   }


}
