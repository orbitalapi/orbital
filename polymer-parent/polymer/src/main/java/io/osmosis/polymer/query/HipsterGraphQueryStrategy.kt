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
import io.osmosis.polymer.query.graph.description
import io.osmosis.polymer.schemas.Link
import io.osmosis.polymer.schemas.Path
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.schemas.describe
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
      val evaluationResult = queryContext.startChild(this, "Evaluating ${edge.description()} with evaluator ${evaluator.javaClass.simpleName}") {
         evaluator.evaluate(edge, queryContext)
      }
      log().debug("Evaluated ${evaluationResult.description()}")
      if (evaluationResult.wasSuccessful) {
         return evaluationResult
      } else {
         throw SearchFailedException("Could not evaluate edge $edge: ${evaluationResult.error!!}", queryContext.evaluatedPath(),queryContext.profiler.root)
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
//      log().debug("Current graph state: \n ${graph.description()}")

      return queryContext.startChild(this, "Searching for path $searchDescription") { op ->
         op.addContext("Current graph state", graph.edgeDescriptions())
         doSearch(start, target, graph, searchDescription, queryContext, op)
      }
   }

   private fun doSearch(start: Element, target: Element, graph: HipsterDirectedGraph<Element, Relationship>, searchDescription: String, queryContext: QueryContext, op: ProfilerOperation): Pair<TypedInstance, Path>? {
      val searchResult = graphSearch(start, target, graph)
      if (searchResult.state() != target) {
         // Search failed, and couldn't match the node
         log().debug("Search failed: $searchDescription. Nearest match was ${searchResult.state()}")
         log().debug("Search failed path: \n${searchResult.path().convertToPolymerPath(start, target).description.split(",").joinToString("\n")}")
         return null
      }

      // TODO : validate this is a valid path
      log().debug("Search $searchDescription found path: \n ${searchResult.path().describe()}")
      op.addContext("discoveredPath", searchResult.path().describeLinks())
      val evaluatedPath = evaluatePath(searchResult, queryContext)
      val resultValue = selectResultValue(evaluatedPath, queryContext, target)
      val path = searchResult.path().convertToPolymerPath(start, target)
      return if (resultValue != null) {
         resultValue to path
      } else { // Search failed
         null
      }
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

private fun List<WeightedNode<Relationship, Element, Double>>.toLinks(): List<Link> {
   return this.mapIndexed { index, weightedNode ->
      if (index == 0) {
         null
      } else {
         val fromElement = this[index - 1].state()
         val toElement = this[index].state()
         val action = this[index].action()
         Link(fromElement.valueAsQualifiedName(), action, toElement.valueAsQualifiedName(), this[index].cost.toInt())
      }
   }.toList().filterNotNull()
}

private fun List<WeightedNode<Relationship, Element, Double>>.describe(): String {
   return this.toLinks().describe()
}
private fun List<WeightedNode<Relationship, Element, Double>>.describeLinks(): List<String> {
   return this.toLinks().map { it.toString() }
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

private fun <V, E> HipsterDirectedGraph<V, E>.edgeDescriptions(): List<String> {
   return this.vertices().flatMap {
      this.outgoingEdgesOf(it).map { edge ->
         "${edge.vertex1} -[${edge.edgeValue}]-> ${edge.vertex2}"
      }
   }
}
private fun <V, E> HipsterDirectedGraph<V, E>.description(): String? {
   return this.edgeDescriptions().joinToString("\n")
}


