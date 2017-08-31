package io.osmosis.polymer.query

import es.usc.citius.hipster.algorithm.Algorithm
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import es.usc.citius.hipster.model.Transition
import es.usc.citius.hipster.model.impl.WeightedNode
import es.usc.citius.hipster.model.problem.ProblemBuilder
import io.osmosis.polymer.*
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.graph.EdgeEvaluator
import io.osmosis.polymer.query.graph.EvaluatedEdge
import io.osmosis.polymer.schemas.Link
import io.osmosis.polymer.schemas.Path
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.schemas.description
import io.osmosis.polymer.utils.log

class EdgeNavigator(linkEvaluators: List<EdgeEvaluator>) {
   private val evaluators = linkEvaluators.associateBy { it.relationship }
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

class HipsterGraphQueryStrategy( private val edgeEvaluator: EdgeNavigator) : QueryStrategy {

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
      var lastResult: Pair<TypedInstance,Path>?
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

   private fun search(start: Element, target: Element, queryContext: QueryContext): Pair<TypedInstance, Path>? {
      data class EvaluateRelationshipAction(val relationship: Relationship, val target: Element)

      val graph = PolymerGraphBuilder(queryContext.schema).build(queryContext.facts)
      val searchDescription = "$start -> $target"
      log().debug("Searching for path from $searchDescription")
      log().debug("Current graph state: \n ${graph.description()}")
      val searchProblem = ProblemBuilder.create()
         .initialState(start)
         .defineProblemWithExplicitActions()
         .useActionFunction({ element ->
            // Find all the relationships that we consider traversable right now
            val edges = graph.outgoingEdgesOf(element.graphNode())
            edges.filter { canBeEvaluated(it, queryContext) }
               .map { EvaluateRelationshipAction(it.edgeValue, it.vertex2) }
         })
         .useTransitionFunction { action, fromElement ->
            val edge: GraphEdge<Element, Relationship> = graph.outgoingEdgesOf(fromElement.graphNode())
               .firstOrNull { it.edgeValue == action.relationship && it.vertex2 == action.target }
               ?: error("No match found")
            val evaluatedEdge = edgeEvaluator.evaluate(edge, queryContext)
            queryContext.addEvaluatedEdge(evaluatedEdge)

            // Return the .graphNode().  If this result was an instance, the value itself
            // isn't present in the graph, but it's type is.  If we return the instance, it's
            // absence from the graph causes the search to terminate prematurely
            evaluatedEdge.result!!.graphNode()
         }
         .useCostFunction { transition: Transition<EvaluateRelationshipAction, Element>? ->
            // TODO ...  For now, cost is constant, but should come from the relationship
            1.0
         }
         .build()

      val searchCompletedFn = { node: WeightedNode<EvaluateRelationshipAction, Element, Double> ->
         val state = node.state()
         val matched = state.elementType == ElementType.TYPE
            && (state.value) == target.value
         if (matched) {
            log().debug("Search $searchDescription completed successfully")
         }
         matched
      }

      try {
//         public static <A, S, C extends Comparable<C>, N extends HeuristicNode<A, S, C, N>> AStar<A, S, C, N> createAStar(
//         SearchProblem<A, S, N> components) {

         val searchResult = Hipster.createAStar(searchProblem).search(searchCompletedFn)
         // This is a naive impl., but it'll work until it doesn't.
         // Find an instance of desired return type in the context.
         // It'd be better to walk the state backwards (presumably at most 1 node)
         // and examine the results of the evaluations\
         // In the case of a PROVIDES evaluation, the result won't be present in the graph, so
         // need to allow the previous evaluations to return multiple values - the value in the graph (the type),
         // and the real return value (the instance)
         // TODO : Make the path accessible
         val path = searchResult.recreatePath(start, target, graph)
         val targetType = queryContext.schema.type(target.value.toString())
         val resultInstance = queryContext.getFact(targetType)
         return if (searchResult.goalNode.state() == target) {
            resultInstance to path
         } else { // failed
            null
         }
      } catch (e: SearchFailedException) {
         log().error("Failed to execute search: ${e.message}")
         log().error("Evaluated path: ${e.evaluatedPath.description()}")
         throw e
      }
   }

   private fun canBeEvaluated(edge: GraphEdge<Element, Relationship>, queryContext: QueryContext): Boolean {
      return when (edge.edgeValue) {
         Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE -> {
            val (declaringType, attributeType) = queryContext.schema.attribute(edge.vertex2.value as String)
            queryContext.hasFactOfType(declaringType)
         }
         else -> true
      }
   }
}

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


