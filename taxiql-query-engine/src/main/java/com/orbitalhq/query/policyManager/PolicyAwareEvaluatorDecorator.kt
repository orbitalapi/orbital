package com.orbitalhq.query.policyManager

import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.graph.edges.EdgeEvaluator
import com.orbitalhq.query.graph.edges.EvaluatableEdge
import com.orbitalhq.query.graph.edges.EvaluatedEdge
import com.orbitalhq.schemas.Relationship

class PolicyAwareEvaluatorDecorator(private val evaluator: EdgeEvaluator) : EdgeEvaluator {
   override val relationship: Relationship = evaluator.relationship

   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

}
