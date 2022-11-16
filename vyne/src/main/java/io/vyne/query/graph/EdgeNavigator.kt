package io.vyne.query.graph

import com.google.common.base.Stopwatch
import io.vyne.query.QueryContext
import io.vyne.query.graph.edges.EdgeEvaluator
import io.vyne.query.graph.edges.EvaluatableEdge
import io.vyne.query.graph.edges.EvaluatedEdge
import io.vyne.utils.StrategyPerformanceProfiler

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
