package io.vyne

import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.HashBasedHipsterDirectedGraph
import es.usc.citius.hipster.model.Transition
import es.usc.citius.hipster.model.impl.WeightedNode
import es.usc.citius.hipster.model.problem.ProblemBuilder
import io.vyne.query.graph.EvaluatedPathSet
import io.vyne.query.graph.Element
import io.vyne.query.graph.pathHashExcludingWeights
import io.vyne.schemas.Relationship
import io.vyne.utils.ImmutableEquality
import io.vyne.utils.cached
import mu.KotlinLogging
import java.util.HashMap

private val logger = KotlinLogging.logger {}

/**
 * An extension of the HipsterDirectedGraph which
 * caches exact searches, in order to reduce calls to find()
 */
class SchemaPathFindingGraph(connections: HashMap<Element, Set<GraphEdge<Element, Relationship>>>) :
   VyneHashBasedHipsterDirectedGraph<Element, Relationship>() {
   init {
      connected = connections
   }

   private data class SearchCacheKey(
      val startFact: Element,
      val targetFact: Element,
      val evaluatedEdges: EvaluatedPathSet
   ) {
      val equality =
         ImmutableEquality(this, SearchCacheKey::startFact, SearchCacheKey::targetFact, SearchCacheKey::evaluatedEdges)

      override fun equals(other: Any?): Boolean {
         return equality.isEqualTo(other)
      }

      override fun hashCode(): Int {
         return equality.hash()
      }
   }

   private fun doSearch(key:SearchCacheKey): WeightedNode<Relationship, Element, Double>? {
      // Construct a specialised search problem, which allows us to supply a custom cost function.
      // The cost function applies a higher 'cost' to the nodes transitions that have previously been attempted.
      // (In earlier versions, we simply remvoed edges after failed attempts)
      // This means that transitions that have been tried in a path become less favoured (but still evaluatable)
      // than transitions that haven't been tried.
      val problem = ProblemBuilder.create()
         .initialState(key.startFact)
         .defineProblemWithExplicitActions()
         .useTransitionFunction { state ->
            outgoingEdgesOf(state).map { edge ->
               Transition.create(state, edge.edgeValue, edge.vertex2)
            }
         }
         .useCostFunction { transition ->
            key.evaluatedEdges.calculateTransitionCost(transition.fromState, transition.action, transition.state)
         }
         .build()


      /*val executionPath = Hipster
            .createDijkstra(problem)
            .search(key.targetFact).goalNode*/
      val executionPath = VyneGraphSearchAlgorithm
         .create(problem, key.evaluatedEdges)
         .search(key.targetFact).goalNode




      return if (executionPath.state() != key.targetFact) {
         logger.debug { "No path found between ${key.startFact} and ${key.targetFact}" }
         null
      } else {
         logger.debug { "Generated path with hash ${executionPath.pathHashExcludingWeights()}" }
         executionPath
      }
   }
   private val searchCache = cached { key: SearchCacheKey ->
     doSearch(key)
   }

   fun findPath(
      startFact: Element,
      targetFact: Element,
      evaluatedEdges: EvaluatedPathSet
   ): WeightedNode<Relationship, Element, Double>? {
      val key = SearchCacheKey(startFact, targetFact, evaluatedEdges)
      return searchCache.get(key)

   }
}

open class VyneHashBasedHipsterDirectedGraph<V, E>(
) : HashBasedHipsterDirectedGraph<V, E>() {


   // val addVertexTimings = mutableListOf<Pair<V, Long>>()
   // val connectTimings = mutableListOf<Pair<GraphEdge<V, E>, Long>>()
   override fun connect(v1: V, v2: V, value: E): GraphEdge<V, E> {
      val edge = buildEdge(v1, v2, value)
      // Associate the vertices with their edge
      connected[v1]!!.add(edge)
      connected[v2]!!.add(edge)
      return edge
   }

   fun connect(edge: GraphEdge<V, E>) {
      connected[edge.vertex1]!!.add(edge)
      connected[edge.vertex2]!!.add(edge)
   }

   @Deprecated("Call create(Connections) instead, as this method is slow")
   override fun add(v: V): Boolean {
      //add a new entry to the hash map if it does not exist
      var retValue = false
      connected.computeIfAbsent(v, {
         retValue = true
         HashSet()
      })
      return retValue
   }

   fun prune(vertices: List<V>) {
      vertices.forEach { vertex ->
         this.connected.remove(vertex)
      }
   }

   fun addRemovedEdges(removedEdges: List<GraphEdge<V, E>>) {
      removedEdges.forEach { edge ->
         if (connected.containsKey(edge.vertex1) && connected.containsKey(edge.vertex2)) {
            this.connect(edge.vertex1, edge.vertex2, edge.edgeValue)
         }
      }
   }


   companion object {
      fun <V, E> create() =
         VyneHashBasedHipsterDirectedGraph<V, E>()

      // This approach yeilds minor performance improvements
      // by allocating sizing up-front, and replacing thread-safety
      // with up-front building.
      // Do not attempt to modify this graph once it's been built
      fun  createCachingGraph(connections: List<HipsterGraphBuilder.Connection<Element, Relationship>>): SchemaPathFindingGraph {
         val maps = HashMap<Element, Set<GraphEdge<Element,Relationship>>>(connections.size * 2)
         connections.forEach {
            maps[it.vertex1] = mutableSetOf()
            maps[it.vertex2] = mutableSetOf()
         }
         val graph = SchemaPathFindingGraph(maps)
         connections.forEach { connection -> graph.connect(connection) }
         return graph
      }

   }
}
