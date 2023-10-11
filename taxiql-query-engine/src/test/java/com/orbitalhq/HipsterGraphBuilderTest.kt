package com.orbitalhq

import com.orbitalhq.query.graph.Element
import com.orbitalhq.query.graph.type
import com.orbitalhq.schemas.Relationship
import com.orbitalhq.utils.Benchmark
import org.junit.Test

class HipsterGraphBuilderTest {

   @Test
   fun `benchmark creating a graph with a large number of connections`() {
      val connections = (0..10_000).map { index ->
         HipsterGraphBuilder.Connection(
            type("type_$index"),
            type("type_${index - 1}"),
            Relationship.IS_TYPE_OF
         )
      }

      Benchmark.benchmark("create hipster graph", warmup = 10, iterations = 50) {
         HipsterGraphBuilder.create<Element,Relationship>()
            .createDirectedGraph(connections)
      }

   }
}
