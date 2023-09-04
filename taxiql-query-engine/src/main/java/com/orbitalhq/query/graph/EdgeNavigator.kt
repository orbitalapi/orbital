package com.orbitalhq.query.graph

import com.google.common.base.Stopwatch
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.graph.edges.EdgeEvaluator
import com.orbitalhq.query.graph.edges.EvaluatableEdge
import com.orbitalhq.query.graph.edges.EvaluatedEdge
import com.orbitalhq.utils.StrategyPerformanceProfiler

class EdgeNavigator(linkEvaluators: List<EdgeEvaluator>) {
   private val evaluators = linkEvaluators.associateBy { it.relationship }

   suspend fun evaluate(edge: EvaluatableEdge, queryContext: QueryContext): EvaluatedEdge {
      val relationship = edge.relationship
      val evaluator = evaluators[relationship]
         ?: error("No LinkEvaluator provided for relationship ${relationship.name}")
      val sw = Stopwatch.createStarted()
      val evaluationResult = evaluator.evaluate(edge, queryContext)
       StrategyPerformanceProfiler.record("Hipster.evaluate.${evaluator.relationship}", sw.elapsed())
      return evaluationResult
   }
}
