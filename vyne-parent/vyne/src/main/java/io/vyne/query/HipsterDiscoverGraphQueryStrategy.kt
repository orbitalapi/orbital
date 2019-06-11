package io.vyne.query

import es.usc.citius.hipster.algorithm.Algorithm
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.GraphSearchProblem
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.*
import io.vyne.models.TypedInstance
import io.vyne.query.graph.*
import io.vyne.schemas.Link
import io.vyne.schemas.Path
import io.vyne.schemas.Relationship
import io.vyne.schemas.describe
import io.vyne.utils.log

class EdgeNavigator(linkEvaluators: List<EdgeEvaluator>) {
   private val evaluators = linkEvaluators.associateBy { it.relationship }

//   fun evaluate(startNode: Element, relationship: Relationship, endNode: Element, queryContext: QueryContext): EvaluatedEdge {
////      val relationship = edge.edgeValue
//      val evaluator = evaluators[relationship] ?:
//      error("No LinkEvaluator provided for relationship ${relationship.name}")
//      val evaluationResult = queryContext.startChild(this, "Evaluating ${edge.description()} with evaluator ${evaluator.javaClass.simpleName}") {
//         evaluator.evaluate(edge, queryContext)
//      }
//      log().debug("Evaluated ${evaluationResult.description()}")
//      if (evaluationResult.wasSuccessful) {
//         return evaluationResult
//      } else {
//         throw SearchFailedException("Could not evaluate edge $edge: ${evaluationResult.error!!}", queryContext.evaluatedPath(),queryContext.profiler.root)
//      }
//
//      return evaluate(StubEdge(startNode, relationship, endNode), queryContext)
//   }

   fun evaluate(edge: EvaluatableEdge, queryContext: QueryContext): EvaluatedEdge {
      val relationship = edge.relationship
      val evaluator = evaluators[relationship]
         ?: error("No LinkEvaluator provided for relationship ${relationship.name}")
      val evaluationResult = queryContext.startChild(this, "Evaluating ${edge.description} with evaluator ${evaluator.javaClass.simpleName}", OperationType.GRAPH_TRAVERSAL) {
         evaluator.evaluate(edge, queryContext)
      }
      log().debug("Evaluated ${evaluationResult.description()}")
      if (evaluationResult.wasSuccessful) {
         return evaluationResult
      } else {
         throw SearchFailedException("Could not evaluate edge $edge: ${evaluationResult.error!!}", queryContext.evaluatedPath(), queryContext.profiler.root)
      }
   }
}


class HipsterDiscoverGraphQueryStrategy(private val edgeEvaluator: EdgeNavigator) : QueryStrategy {

   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {

      return find(target, context)
   }

//
//   override fun find(expression: String, factSet: Set<TypedInstance>): QueryResult {
//      val target = QueryParser(schema).parse(expression)
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

      // We only support DISCOVER_ONE mode here.
      if (target.mode != QueryMode.DISCOVER) return QueryStrategyResult.empty()

      if (context.facts.isEmpty()) {
         log().info("Cannot perform a graph search, as no facts provied to serve as starting point. ")
         return QueryStrategyResult.empty()
      }

      val targetElement = type(target.type)

      // search from every fact in the context
      var lastResult: Pair<TypedInstance, Path>? = find(targetElement, context)
      return if (lastResult != null) {
         QueryStrategyResult(mapOf(target to lastResult.first))
      } else {
         QueryStrategyResult.empty()
      }
   }

   internal fun find(targetElement: Element, context: QueryContext): Pair<TypedInstance, Path>? {
      val factItr = context.facts.iterator()
      var lastResult: Pair<TypedInstance, Path>?
      do {
         val start = instanceOfType(factItr.next().type)
         lastResult = search(start, targetElement, context)
      } while (factItr.hasNext() && lastResult == null)
      return lastResult
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

   internal fun search(start: Element, target: Element, queryContext: QueryContext): Pair<TypedInstance, Path>? {
      // TODO : This is expensive.  We should cache against the schema.
      val graph = queryContext.startChild(this, "Building graph", OperationType.GRAPH_BUILDING) {
         VyneGraphBuilder(queryContext.schema).build(queryContext.facts)
      }
      val searchDescription = "$start -> $target"
      log().debug("Searching for path from $searchDescription")
//      log().debug("Current graph state: \n ${graph.description()}")

      return queryContext.startChild(this, "Searching for path ${start.valueAsQualifiedName().name} -> ${target.valueAsQualifiedName().name}", OperationType.GRAPH_TRAVERSAL) { op ->
         op.addContext("Current graph", graph.edgeDescriptions())
         doSearch(start, target, graph, searchDescription, queryContext, op)
      }
   }

   private fun doSearch(start: Element, target: Element, graph: HipsterDirectedGraph<Element, Relationship>, searchDescription: String, queryContext: QueryContext, op: ProfilerOperation): Pair<TypedInstance, Path>? {
      val searchResult = graphSearch(start, target, graph)
      if (searchResult.state() != target) {
         // Search failed, and couldn't match the node
         log().debug("Search failed: $searchDescription. Nearest match was ${searchResult.state()}")
         log().debug("Search failed path: \n${searchResult.path().convertToVynePath(start, target).description.split(",").joinToString("\n")}")
         return null
      }

      // TODO : validate this is a valid path
      log().debug("Search $searchDescription found path: \n ${searchResult.path().describe()}")
      op.addContext("Discovered path", searchResult.path().describeLinks())
      val evaluatedPath = evaluatePath(searchResult, queryContext)
      val resultValue = selectResultValue(evaluatedPath, queryContext, target)
      val path = searchResult.path().convertToVynePath(start, target)
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

   private fun selectResultValue(evaluatedPath: List<PathEvaluation>, queryContext: QueryContext, target: Element): TypedInstance? {
      val targetType = queryContext.schema.type(target.value.toString())

      // If the last node in the evaluated path is the type we're after, use that.
      val lastEdgeResult = evaluatedPath.last().resultValue
      if (lastEdgeResult != null && lastEdgeResult.type.matches(targetType)) {
         return lastEdgeResult
      }


      if (queryContext.hasFactOfType(targetType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)) {
         return queryContext.getFact(targetType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
      }

      // The search probably failed
      return null
   }

   private fun evaluatePath(searchResult: WeightedNode<Relationship, Element, Double>, queryContext: QueryContext): List<PathEvaluation> {
      // The actual result of this isn't directly used.  But the queryContext is updated with
      // nodes as they're discovered (eg., through service invocation)
      val evaluatedEdges = mutableListOf<PathEvaluation>(
         getStartingEdge(searchResult, queryContext)
      )
      searchResult.path()
         .drop(1)
         .mapIndexedTo(evaluatedEdges) { index, weightedNode ->
            // Note re index:  We dropped 1, so indexes are out-by-one.
            // Normally the lastValue would be index-1, but here, it's just index.
            val lastResult = evaluatedEdges[index]
            val endNode = weightedNode.state()
            val evaluationResult = edgeEvaluator.evaluate(EvaluatableEdge(lastResult, weightedNode.action(), endNode), queryContext)
            evaluationResult
         }

      return evaluatedEdges
   }

   fun getStartingEdge(searchResult: WeightedNode<Relationship, Element, Double>, queryContext: QueryContext): StartingEdge {
      val firstNode = searchResult.path().first()
      val firstType = queryContext.schema.type(firstNode.state().valueAsQualifiedName())
      require(queryContext.hasFactOfType(firstType)) { "The queryContext doesn't have a fact present of type ${firstType.fullyQualifiedName}, but this is the starting point of the discovered solution." }
      val firstFact = queryContext.getFact(firstType)
      val startingEdge = StartingEdge(firstFact, firstNode.state())
      return startingEdge
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

private fun List<WeightedNode<Relationship, Element, Double>>.convertToVynePath(start: Element, target: Element): Path {
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
//private fun List<WeightedNode<HipsterDiscoverGraphQueryStrategy.EvaluateRelationshipAction, Element, Double>>.convertToVynePath(start: Element, target: Element): Path {
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
         val edge = graph.outgoingEdgesOf(fromElement).firstOrNull { it.vertex2 == toElement }
            ?: throw IllegalStateException("No edge found from $fromElement -> $toElement, but they were adjoining nodes in the result")
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


