package io.vyne.query.policyManager

import io.vyne.query.QueryContext
import io.vyne.query.graph.edges.EvaluatableEdge
import io.vyne.query.graph.edges.EdgeEvaluator
import io.vyne.query.graph.edges.EvaluatedEdge
import io.vyne.schemas.Relationship

class PolicyAwareEvaluatorDecorator(private val evaluator: EdgeEvaluator) : EdgeEvaluator {
   override val relationship: Relationship = evaluator.relationship

   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

}
