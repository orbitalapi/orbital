package io.vyne.query.graph.edges

import io.vyne.query.QueryContext
import io.vyne.schemas.Relationship

interface EdgeEvaluator {
   val relationship: Relationship
   suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge
}
