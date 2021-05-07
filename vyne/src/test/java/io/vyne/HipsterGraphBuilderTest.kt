package io.vyne

import io.vyne.query.graph.Element
import io.vyne.query.graph.type
import io.vyne.schemas.Relationship
import io.vyne.utils.Benchmark
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
