package io.vyne

import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.HashBasedHipsterDirectedGraph
import io.vyne.query.graph.Element
import io.vyne.utils.timed
import java.util.LinkedHashSet
import java.util.function.Function

class VyneHashBasedHipsterDirectedGraph<V, E> : HashBasedHipsterDirectedGraph<V, E>() {
   // val addVertexTimings = mutableListOf<Pair<V, Long>>()
   // val connectTimings = mutableListOf<Pair<GraphEdge<V, E>, Long>>()
   override fun connect(v1: V, v2: V, value: E): GraphEdge<V, E> {
      val edge = buildEdge(v1, v2, value)
      // Associate the vertices with their edge
      connected[v1]!!.add(edge)
      connected[v2]!!.add(edge)
      return edge
   }

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
      fun <V, E> create() = VyneHashBasedHipsterDirectedGraph<V, E>()
   }
}
