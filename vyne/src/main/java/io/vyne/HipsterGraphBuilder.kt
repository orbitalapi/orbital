package io.vyne

/**
 * THIS IS A DIRECT COPY OF [es.usc.citius.hipster.graph.GraphBuilder]
 * The only part that's different is that when appending a connection, we ensure that
 * a duplicate connection doesn't alredy exists between the two nodes.
 * Hipster doesn't support mulitple connections.
 * this allows us to fail fast.
 *
 * Original:  When adding a connection, look for matches on v1 & v2 (ignoring e), and mutate the value of e.
 * This fails if v1 & v2 are equal, but the intended value of e is different, resulting in
 * e remaining as Object.
 * TODO : Raise an issue with the Hipster chaps, they seem lovely.
 */

/*
 * Copyright 2014 CITIUS <http://citius.usc.es>, University of Santiago de Compostela.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import es.usc.citius.hipster.graph.HashBasedHipsterGraph
import es.usc.citius.hipster.graph.HipsterGraph
import lang.taxi.ImmutableEquality

/**
 *
 * Graph builder assistant to create a Hipster graph. Usage example:
 * <pre class="prettyprint">
 * `HipsterGraph<String,Double> =
 * HipsterGraphBuilder.<String,Double>create()
 * .connect("A").to("B").withEdge(4d)
 * .connect("A").to("C").withEdge(2d)
 * .connect("B").to("C").withEdge(5d)
 * .createDirectedGraph();
` *
</pre> *
 */
class HipsterGraphBuilder<V, E> private constructor(
   private val existingConnections: MutableMap<Pair<V, V>, E> = mutableMapOf(),
   private val connections: MutableMap<Connection<V, E>, Connection<V, E>> = mutableMapOf()
) {

   fun copy(): HipsterGraphBuilder<V, E> = create(this.existingConnections, this.connections)

   data class Connection<V, E>(val vertex1: V, val vertex2: V, val edge: E) {
      val equality =
         ImmutableEquality(this, Connection<*, *>::vertex1, Connection<*, *>::vertex2, Connection<*, *>::edge)

      override fun equals(other: Any?): Boolean {
         return equality.isEqualTo(other)
      }

      override fun hashCode(): Int {
         return equality.hash()
      }
   }

   fun createDirectedGraph(connections: List<Connection<V, E>>): VyneHashBasedHipsterDirectedGraph<V, E> {
      val graph = VyneHashBasedHipsterDirectedGraph.create<V, E>()
      connections.forEach { connection ->
         graph.add(connection.vertex1)
         graph.add(connection.vertex2)
         graph.connect(connection.vertex1, connection.vertex2, connection.edge)
      }
      return graph
   }

   fun createUndirectedGraph(): HipsterGraph<V, E> {
      val graph = HashBasedHipsterGraph.create<V, E>()
      for (c in connections.values) {
         graph.add(c.vertex1)
         graph.add(c.vertex2)
         graph.connect(c.vertex1!!, c.vertex2!!, c.edge)
      }
      return graph
   }


   inner class Vertex1 internal constructor(internal var vertex1: V) {

      fun to(vertex: V): Vertex2 {
         return Vertex2(vertex)
      }

      inner class Vertex2 internal constructor(internal var vertex2: V) {

         fun withEdge(edge: E): HipsterGraphBuilder<V, E> {
            // TODO : This may cause problems, as the original behaviour
            // prevented v1 -> v2 duplicates with different edges, by silently screwing up.
            // This is changed.  To be consistent, I should check
            // if any v1 -> v2 instances exist, and fail with an error.

            // First, check if the connection already exists with a different edge
            val vertexPair = vertex1 to vertex2
            if (existingConnections.containsKey(vertexPair)) {
//               log().warn("$vertex1 -> $vertex2 already exists with connection ${existingConnections[vertexPair]}.  Trying to add another connection of type $edge ")
               // do nothing
               return this@HipsterGraphBuilder
               // tODO : Create a multi-link relationship
            }
            val connection = Connection(vertex1, vertex2, edge)
            val existingConnection = connections[connection]
            if (existingConnection != null) {
               // Do nothing.
//               connections[connectionIndex].edge = edge
            } else {
//               connection.edge = edge
               existingConnections.put(vertexPair, edge)
               connections[connection] = connection
            }
            return this@HipsterGraphBuilder
         }
      }
   }

   companion object {

      fun <V, E> create(): HipsterGraphBuilder<V, E> {
         return HipsterGraphBuilder()
      }

      fun <V, E> create(
         existingConnections: MutableMap<Pair<V, V>, E>,
         connections: MutableMap<Connection<V, E>, Connection<V, E>>
      ): HipsterGraphBuilder<V, E> {
         return HipsterGraphBuilder(LinkedHashMap(existingConnections), LinkedHashMap(connections))
      }
   }

}
