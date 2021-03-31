package io.vyne.query.policyManager

import io.vyne.query.QueryContext
import io.vyne.query.graph.EdgeEvaluator
import io.vyne.query.graph.EvaluatableEdge
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.schemas.Relationship
import kotlinx.coroutines.flow.Flow

class PolicyAwareEvaluatorDecorator(private val evaluator: EdgeEvaluator) : EdgeEvaluator {
   override val relationship: Relationship = evaluator.relationship

   override fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

}
