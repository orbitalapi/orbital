package com.orbitalhq.query.graph.edges

import com.orbitalhq.query.QueryContext
import com.orbitalhq.schemas.Relationship

interface EdgeEvaluator {
   val relationship: Relationship
   suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge
}
