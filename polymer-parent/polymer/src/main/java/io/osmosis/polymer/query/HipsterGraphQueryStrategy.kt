package io.osmosis.polymer.query

import es.usc.citius.hipster.algorithm.Algorithm
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.GraphSearchProblem
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import es.usc.citius.hipster.model.impl.WeightedNode
import io.osmosis.polymer.Element
import io.osmosis.polymer.PolymerGraphBuilder
import io.osmosis.polymer.instance
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.graph.EdgeEvaluator
import io.osmosis.polymer.query.graph.EvaluatedEdge
import io.osmosis.polymer.schemas.Link
import io.osmosis.polymer.schemas.Path
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.type
import io.osmosis.polymer.utils.log

class EdgeNavigator(linkEvaluators: List<EdgeEvaluator>) {
   private val evaluators = linkEvaluators.associateBy { it.relationship }

   data class StubEdge(val start: Element, val relationship: Relationship, val endNode: Element) : GraphEdge<Element, Relationship> {
      override fun getEdgeValue(): Relationship = relationship
      override fun getVertex2() = endNode
      override fun getVertex1() = start
      override fun getType(): GraphEdge.Type = GraphEdge.Type.DIRECTED
   }

   fun evaluate(startNode: Element, relationship: Relationship, endNode: Element, queryContext: QueryContext): EvaluatedEdge {
      return evaluate(StubEdge(startNode, relationship, endNode), queryContext)
   }

   fun evaluate(edge: GraphEdge<Element, Relationship>, queryContext: QueryContext): EvaluatedEdge {
      val relationship = edge.edgeValue
      val evaluator = evaluators[relationship] ?:
         error("No LinkEvaluator provided for relationship ${relationship.name}")
      val evaluationResult = evaluator.evaluate(edge, queryContext)
      if (evaluationResult.wasSuccessful) {
         return evaluationResult
      } else {
         throw SearchFailedException("Could not evaluate edge $edge: ${evaluationResult.error!!}", queryContext.evaluatedPath())
      }
   }
}

class HipsterGraphQueryStrategy(private val edgeEvaluator: EdgeNavigator) : QueryStrategy {

   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      return find(target, context)
   }

//
//   override fun find(queryString: String, factSet: Set<TypedInstance>): QueryResult {
//      val target = QueryParser(schema).parse(queryString)
//      return find(target, factSet)
//   }
//
//   override fun find(target: QuerySpecTypeNode, factSet: Set<TypedInstance>): QueryResult {
//      return find(setOf(target), factSet)
//   }
//
//   override fun find(target: Set<QuerySpecTypeNode>, factSet: Set<TypedInstance>): QueryResult {
//      return find(target, queryContext(factSet))
//   }

   fun find(targets: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      // Note : There is an existing, working impl. of this in QueryEngine (the OrientDB approach),
      // but I haven't gotten around to copying it yet.
      if (targets.size != 1) TODO("Support for target sets not yet built")
      val target = targets.first()

      if (context.facts.isEmpty()) error("Context must have at least one fact, as these provide the starting points for searches")

      // search from every fact in the context
      val factItr = context.facts.iterator()
      var lastResult: Pair<TypedInstance, Path>?
      val targetElement = type(target.type)

      do {
         val start = instance(factItr.next())
         lastResult = search(start, targetElement, context)
      } while (factItr.hasNext() && lastResult == null)

      return QueryStrategyResult(mapOf(target to lastResult?.first))
   }

//   override fun queryContext(factSet: Set<TypedInstance>): QueryContext {
//      return QueryContext(schema, (this.models + factSet).toMutableSet(), this)
//   }
//
//   override fun findPath(start: QualifiedName, target: QualifiedName): Path {
//      return search(type(start.fullyQualifiedName), type(target.fullyQualifiedName), queryContext()).path
//   }
//
//   override fun findPath(start: String, target: String): Path {
//      return search(type(start), type(target), queryContext()).path
//   }
//
//   override fun findPath(start: Type, target: Type): Path {
//      return search(type(start.fullyQualifiedName), type(target.fullyQualifiedName), queryContext()).path
//   }
//
//   fun findPath(start: Element, target: Element) {
//      search(start, target, queryContext())
//   }

   data class EvaluateRelationshipAction(val relationship: Relationship, val target: Element)

   private fun search(start: Element, target: Element, queryContext: QueryContext): Pair<TypedInstance, Path>? {
      val graph = PolymerGraphBuilder(queryContext.schema).build(queryContext.facts)
      val searchDescription = "$start -> $target"
      log().debug("Searching for path from $searchDescription")
      log().debug("Current graph state: \n ${graph.description()}")


//      val searchProblem = ProblemBuilder.create()
//         .initialState(start)
//         .defineProblemWithExplicitActions()
//         .useActionFunction({ element ->
//            // Find all the relationships that we consider traversable right now
//            val edges = graph.outgoingEdgesOf(element.graphNode())
//            edges.filter { canBeEvaluated(it, queryContext) }
//               .map { EvaluateRelationshipAction(it.edgeValue, it.vertex2) }
//         })
//         .useTransitionFunction { action, fromElement ->
//            // Because we're not doing path evaluation within the search, just return
//            // the target node for now.
//            action.target
//         }
//         .useCostFunction { transition ->
//            1.0 // Use a constant cost for now
//         }
//         .build()
      val searchResult = graphSearch(start, target, graph)
//      val searchResult = Hipster.createAStar(searchProblem).search(target)
      if (searchResult.state() != target) {
         // Search failed, and couldn't match the node
         return null
      }

      // TODO : validate this is a valid path

      val evaluatedPath = evaluatePath(searchResult, queryContext)
      val resultValue = selectResultValue(evaluatedPath, queryContext, target)
      val path = searchResult.path().convertToPolymerPath(start, target)
      return if (resultValue != null) {
         resultValue to path
      } else { // Search failed
         null
      }


//         .useTransitionFunction { action, fromElement ->
//            val edge: GraphEdge<Element, Relationship> = graph.outgoingEdgesOf(fromElement.graphNode())
//               .firstOrNull { it.edgeValue == action.relationship && it.vertex2 == action.target }
//               ?: error("No match found")
//            val evaluatedEdge = edgeEvaluator.evaluate(edge, queryContext)
//            queryContext.addEvaluatedEdge(evaluatedEdge)
//
//            // Return the .graphNode().  If this result was an instance, the value itself
//            // isn't present in the graph, but it's type is.  If we return the instance, it's
//            // absence from the graph causes the search to terminate prematurely
//            evaluatedEdge.result!!.graphNode()
//         }
//         .useCostFunction { transition: Transition<EvaluateRelationshipAction, Element>? ->
//            // TODO ...  For now, cost is constant, but should come from the relationship
//            1.0
//         }
//         .build()
//
//      val searchCompletedFn = { node: WeightedNode<EvaluateRelationshipAction, Element, Double> ->
//         val state = node.state()
//         val matched = state.elementType == ElementType.TYPE
//            && (state.value) == target.value
//         if (matched) {
//            log().debug("Search $searchDescription completed successfully")
//         }
//         matched
//      }

//      try {
//         public static <A, S, C extends Comparable<C>, N extends HeuristicNode<A, S, C, N>> AStar<A, S, C, N> createAStar(
//         SearchProblem<A, S, N> components) {

//         val searchResult = Hipster.createAStar(searchProblem).search(searchCompletedFn)
      // This is a naive impl., but it'll work until it doesn't.
      // Find an instance of desired return type in the context.
      // It'd be better to walk the state backwards (presumably at most 1 node)
      // and examine the results of the evaluations\
      // In the case of a PROVIDES evaluation, the result won't be present in the graph, so
      // need to allow the previous evaluations to return multiple values - the value in the graph (the type),
      // and the real return value (the instance)
      // TODO : Make the path accessible
//         val path = searchResult.recreatePath(start, target, graph)
//         val targetType = queryContext.schema.type(target.value.toString())
//         val resultInstance = queryContext.getFact(targetType)
//         return if (searchResult.goalNode.state() == target) {
//            resultInstance to path
//         } else { // failed
//            null
//         }
//      } catch (e: SearchFailedException) {
//         log().error("Failed to execute search: ${e.message}")
//         log().error("Evaluated path: ${e.evaluatedPath.description()}")
//         throw e
//      }
   }

   private fun graphSearch(from: Element, to: Element, graph: HipsterDirectedGraph<Element, Relationship>): WeightedNode<Relationship, Element, Double> {
      val problem = GraphSearchProblem.startingFrom(from).`in`(graph).takeCostsFromEdges().build()
      return Hipster.createAStar(problem).search(to).goalNode
   }

   private fun selectResultValue(evaluatedPath: List<EvaluatedEdge>, queryContext: QueryContext, target: Element): TypedInstance? {
      // If the last node in the evaluated path is the type we're after, use that.
      val lastEdgeResult = evaluatedPath.last().result
      if (lastEdgeResult?.value is TypedInstance && (lastEdgeResult.value as TypedInstance).type == target.value) {
         return lastEdgeResult.value
      }

      val targetType = queryContext.schema.type(target.value.toString())
      if (queryContext.hasFactOfType(targetType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)) {
         return queryContext.getFact(targetType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
      }

      // The search probably failed
      return null
   }

   private fun evaluatePath(searchResult: WeightedNode<Relationship, Element, Double>, queryContext: QueryContext): List<EvaluatedEdge> {
      // The actual result of this isn't directly used.  But the queryContext is updated with
      // nodes as they're discovered (eg., through service invocation)
      return searchResult.path()
         .drop(1) // The first node has no action
         .map { weightedNode ->
            val startNode = weightedNode.previousNode().state()
            val endNode = weightedNode.state()
            val evaluationResult = edgeEvaluator.evaluate(startNode, weightedNode.action(), endNode, queryContext)
            evaluationResult
         }.toList()
   }
//   private fun evaluatePath(searchResult: Algorithm<EvaluateRelationshipAction, Element, WeightedNode<EvaluateRelationshipAction, Element, Double>>.SearchResult, queryContext: QueryContext): List<EvaluatedEdge> {
//      // The actual result of this isn't directly used.  But the queryContext is updated with
//      // nodes as they're discovered (eg., through service invocation)
//      return searchResult.goalNode.path()
//         .drop(1) // The first node has no action
//         .map { weightedNode ->
//            val startNode = weightedNode.previousNode().state()
//            val endNode = weightedNode.state()
//            val evaluationResult = edgeEvaluator.evaluate(startNode, weightedNode.action().relationship, endNode, queryContext)
//            evaluationResult
//         }.toList()
//   }

   private fun canBeEvaluated(edge: GraphEdge<Element, Relationship>, queryContext: QueryContext): Boolean {
      if (edge.edgeValue !is Relationship) {
         val message = "An invalid link has been created between ${edge.vertex1} -> ${edge.vertex2}, as the relationship type is Object.  Hipster4J does this when duplicate links are formed.  Re-check the graph builder to eliminate duplicate links"
         throw IllegalStateException(message)
      }
      return when (edge.edgeValue) {
//         Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE -> {
//            val (declaringType, _) = queryContext.schema.attribute(edge.vertex2.value as String)
//            queryContext.hasFactOfType(declaringType)
//         }
         else -> true
      }
   }
}

private fun List<WeightedNode<Relationship, Element, Double>>.convertToPolymerPath(start: Element, target: Element): Path {
   val links = this.mapIndexed { index, weightedNode ->
      if (index == 0) {
         null
      } else {
         val fromElement = this[index - 1].state()
         val toElement = this[index].state()
         val action = this[index].action()
         Link(fromElement.valueAsQualifiedName(), action, toElement.valueAsQualifiedName(), this[index].cost.toInt())
      }
   }.toList().filterNotNull()
   return Path(start.valueAsQualifiedName(), target.valueAsQualifiedName(), links)
}
//private fun List<WeightedNode<HipsterGraphQueryStrategy.EvaluateRelationshipAction, Element, Double>>.convertToPolymerPath(start: Element, target: Element): Path {
//   val links = this.mapIndexed { index, weightedNode ->
//      if (index == 0) {
//         null
//      } else {
//         val fromElement = this[index - 1].state()
//         val toElement = this[index].state()
//         val action = this[index].action()
//         Link(fromElement.valueAsQualifiedName(), action.relationship, toElement.valueAsQualifiedName(), this[index].cost.toInt())
//      }
//   }.toList().filterNotNull()
//   return Path(start.valueAsQualifiedName(), target.valueAsQualifiedName(), links)
//}


private fun Algorithm<*, Element, *>.SearchResult.recreatePath(start: Element, target: Element, graph: HipsterDirectedGraph<Element, Relationship>): Path {
   val path = this.getOptimalPaths()[0]
   val links = path.mapIndexed { index, vertex ->
      if (index + 1 >= path.size) {
         null
      } else {
         val fromElement = vertex
         val toElement = path[index + 1]
         val edge = graph.outgoingEdgesOf(fromElement).firstOrNull { it.vertex2 == toElement } ?: throw IllegalStateException("No edge found from $fromElement -> $toElement, but they were adjoining nodes in the result")
         Link(fromElement.valueAsQualifiedName(), edge.edgeValue, toElement.valueAsQualifiedName())
      }
   }.filterNotNull()
   return Path(start.valueAsQualifiedName(), target.valueAsQualifiedName(), links)
}

private fun <V, E> HipsterDirectedGraph<V, E>.description(): String? {
   val paths: List<String> = this.vertices().flatMap {
      this.outgoingEdgesOf(it).map { edge ->
         "${edge.vertex1} -[${edge.edgeValue}]-> ${edge.vertex2}"
      }
   }
   return paths.joinToString("\n")
}


