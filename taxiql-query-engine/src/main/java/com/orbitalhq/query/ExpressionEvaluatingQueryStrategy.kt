package com.orbitalhq.query

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.facts.CopyOnWriteFactBag
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.subscribe
import kotlinx.coroutines.flow.toList
import lang.taxi.expressions.ExtensionFunctionExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.types.Arrays

class ExpressionEvaluatingQueryStrategy : QueryStrategy {
   override suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      if (target.size != 1) {
         return QueryStrategyResult.searchFailed()
      }
      val node = target.single()
      val expression = node.expression ?: return QueryStrategyResult.searchFailed()
      if (expression is TypeExpression) {
         // leave this to other strategies
         return QueryStrategyResult.searchFailed()
      }

      // TODO : This level of brittleness won't scale long-term.
      // However, we need to be able to let type expressions which evaluate to a stream be streams.
      // eg: stream { Movie[].filter( .... ) }
      // That's currently only possible by letting them run as queries, rather than evaluating them as expressions.
      // This code will fail to detect functions that are deeply nested, so will need to cross that bridge
      // when we come to it.
      if (expression is ExtensionFunctionExpression && expression.receiverValue is TypeExpression) {
         val typeExpression = expression.receiverValue as TypeExpression
         val queryNode = QuerySpecTypeNode.fromExpression(typeExpression, context.schema)

         val resultFlow = context.find(queryNode).results

         // Ok, we have to do some hopping here.
         // Elsewhere, we're emitting arrays as individual items, to allow parallel processing on them
         // However, the result is that if we asked for an array, we're actually getting values emitted
         // individually.
         // So, we need to collect the array, as the downstream function is expecting to operate on it.
         val flowToExecuteFunctionOn = if (Arrays.isArray(expression.receiverValue.returnType)) {
            flowOf(TypedCollection.from(resultFlow.toList()))
         } else {
            resultFlow
         }
         val flowOfExpressionResult = flowToExecuteFunctionOn
            .map { typedInstance ->
               // At this point, we need to evaluate the provided expression
               val evaluatedExpression =
                  context.evaluate(expression.functionExpression, CopyOnWriteFactBag(typedInstance, context.schema))
               evaluatedExpression
            }
            .filter { instance ->
               // Filter out nulls.
               // This is how filter functions that operate on streams can signal to drop values.
               instance !is TypedNull
            }
         return QueryStrategyResult(flowOfExpressionResult)
      }

      // At this point, we fall back to evaluating the expression.
      // Bear in mind that this is blocking, so heavy evaluations (including those that perform discoveries)
      // could block the system
      val evaluated = context.evaluate(node.expression, context.facts)
      return QueryStrategyResult.from(evaluated)
   }
}
