package io.vyne.query.graph.edges

import io.vyne.query.QueryContext
import io.vyne.schemas.Relationship

class QueryBuildingEvaluator : EdgeEvaluator {
   override val relationship: Relationship = Relationship.CAN_CONSTRUCT_QUERY

   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      TODO("Not yet implemented")
   }
}
