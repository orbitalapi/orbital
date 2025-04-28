package com.orbitalhq.query

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.query.queryBuilders.QueryGrammarQueryBuilder
import com.orbitalhq.query.queryBuilders.TaxiQlGrammarQueryBuilder
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOperation
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.log
import lang.taxi.expressions.Expression
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.ArgumentSelector
import lang.taxi.types.MemberTypeReferenceExpression
import mu.KotlinLogging

class QueryOperationInvocationStrategy(
   invocationService: OperationInvocationService,
   private val queryBuilders: List<QueryGrammarQueryBuilder> = listOf(TaxiQlGrammarQueryBuilder())
) : QueryStrategy, BaseOperationInvocationStrategy(invocationService) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   override suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      val candidateOperations = lookForCandidateQueryOperations(context, target)
      if (candidateOperations.values.all { it.isEmpty() }) {
         return QueryStrategyResult.searchFailed()
      }
      // MP: 28-Apr-25
      // Don't run a query for a single object if there are no constraints.
      // Otherwise, a query for T can end up invoking database queries (that return T[]), and taking
      // the first result.
      if (target.any { !it.type.isCollection && it.dataConstraints.isEmpty() }) {
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
         lookForCandidateQueryOperations(context.schema, querySpecTypeNode, context)
      }
   }

   @VisibleForTesting
   internal fun lookForCandidateQueryOperations(
      schema: Schema,
      target: QuerySpecTypeNode,
      context: QueryContext
   ): Map<RemoteOperation, Map<Parameter, TypedInstance>> {
      val queryOperations = schema.services
         .flatMap {
            it.queryOperations + it.tableOperations.flatMap { tableOperation ->
               tableOperation.queryOperations
            }
         }
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

            queryOperation to grammarBuilder.buildQuery(queryTarget, queryOperation, schema, context)

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
      dataConstraints: List<Constraint>,
      isCovariant: Boolean
   ): Boolean {
      // bail early
      if (dataConstraints.isEmpty()) {
         return true
      }

      // For now, we're only looking at filter operations.  Revisit when we get to aggregations.
      return dataConstraints.all { constraint ->
         when (constraint) {
            is ExpressionConstraint -> canFilterForExpression(constraint.expression, schema, queryOperation.returnType)
            else -> {
               // TODO : Implement support for the other constraints if/when they become needed
               log().warn("Support for data constraint of type ${constraint::class.simpleName} is not yet implemented, so query operations cannot be invoked for this query.")
               false
            }
         }
      }
   }

   /**
    * Examines an expression, determining if the expression can be evaluated against the return type of the
    * operation
    */
   private fun canFilterForExpression(expression: Expression,schema: Schema, operationReturnType: Type): Boolean {
      if (expression !is OperatorExpression) return false
      val components = listOf(expression.lhs, expression.rhs)
      return components.all { expressionPart ->
         when (expressionPart) {
            is LiteralExpression -> true
            is ArgumentSelector -> true
            is OperatorExpression -> canFilterForExpression(expressionPart, schema, operationReturnType)
            is MemberTypeReferenceExpression -> canFilterOnPropertyType(schema, schema.type(expressionPart.targetType), operationReturnType)
            is TypeExpression -> canFilterOnPropertyType(schema, schema.type(expressionPart.type), operationReturnType)

            else -> {
               logger.warn { "Not implemented - detecting if a Query operation can perform a filter satisfying an expression of kind ${expressionPart::class.simpleName} - ${expressionPart.asTaxi()}" }
               false
            }
         }
      }
   }

   private fun canFilterOnPropertyType(
      schema: Schema,
      propertyType: Type,
      operationReturnType: Type
   ): Boolean {
      val operationReturnParameterisedType =
         if (operationReturnType.isCollection) operationReturnType.typeParameters[0] else operationReturnType
      return operationReturnParameterisedType.attributes.values.any { field ->
         val fieldVyneType = field.resolveType(schema)
         fieldVyneType.isAssignableFrom(propertyType)
      }
   }


}
