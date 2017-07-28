package io.osmosis.polymer.query.graph

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.query.QuerySpecTypeNode
import io.osmosis.polymer.query.QueryStrategy
import io.osmosis.polymer.query.QueryStrategyResult
import io.osmosis.polymer.schemas.Path
import org.springframework.stereotype.Component

@Component
class GraphResolutionStrategy(private val pathEvaluator: PathEvaluator) : QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      // TODO : Note, we're only searching for top level nodes
      // TODO :Also, it's possible that a service invocation may resolve other
      // nodes within the target set, so should re-evaluate if a node is
      // still unresolved more frequently
      val matchedNodes: Map<QuerySpecTypeNode, TypedInstance?> = target.map { targetNode: QuerySpecTypeNode ->
         val solutionsToTry: List<Pair<TypedInstance, Path>> = context.facts.map { knownValue -> knownValue to context.queryEngine.findPath(knownValue.type, targetNode.type) }
            .filter { (_, path) -> path.exists }
         val result = findFirstSuccessfulSolution(solutionsToTry, targetNode, context)
         targetNode to result
      }.toMap()
      return QueryStrategyResult(matchedNodes)
   }

   private fun findFirstSuccessfulSolution(solutionsToTry: List<Pair<TypedInstance, Path>>, targetNode: QuerySpecTypeNode, context: QueryContext): TypedInstance? {
      var result: TypedInstance? = null
      val iterator = solutionsToTry.iterator()
      while (iterator.hasNext() && result == null) {
         val solutionToTry = iterator.next()
         result = evaluate(solutionToTry, targetNode, context)
      }
      return result
   }

   private fun evaluate(solutionToTry: Pair<TypedInstance, Path>, targetNode: QuerySpecTypeNode, context: QueryContext): TypedInstance? {
      val (startingPoint, path) = solutionToTry
      val result = pathEvaluator.evaluate(path, startingPoint, context)
      return result.evaluatedLinks.last().result
   }

}
