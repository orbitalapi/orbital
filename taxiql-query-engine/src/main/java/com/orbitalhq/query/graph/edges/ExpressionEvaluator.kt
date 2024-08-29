package com.orbitalhq.query.graph.edges

import com.orbitalhq.models.TypedNull
import com.orbitalhq.query.QueryContext
import com.orbitalhq.schemas.Relationship
import lang.taxi.expressions.Expression

/**
 * Evaluates an expression as part of graph traversal.
 * Used when an expression type is an input to an operation
 */
class ExpressionEvaluator : EdgeEvaluator {
   override val relationship: Relationship = Relationship.EVALUATES_RETURNING

   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      val expression = edge.vertex1.instanceValue as Expression
      val evaluationResult = if (edge.previousValue != null) {
         context.evaluate(expression, edge.previousValue)
      } else {
         context.evaluate(expression)
      }

      return if (evaluationResult is TypedNull) {
         edge.failure(evaluationResult)
      } else {
         edge.success(evaluationResult)
      }
   }
}
