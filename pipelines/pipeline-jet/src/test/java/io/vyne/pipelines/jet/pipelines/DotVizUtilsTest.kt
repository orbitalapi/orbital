package io.vyne.pipelines.jet.pipelines

import com.winterbe.expekt.should
import io.vyne.pipelines.jet.api.DagGraphLink
import io.vyne.pipelines.jet.api.DagGraphNode
import org.junit.Test

class DotVizUtilsTest {
   @Test
   fun `converts dotViz to dag for UI`() {
      val dataset = DotVizUtils.dotVizToGraphNodes("""digraph Pipeline {
	"Ingest from Flux input" -> "Transform Person to Target using Vyne";
	"Transform Person to Target using Vyne" -> "Write to List sink";
}""")
      dataset.nodes.should.equal(
         listOf(
            DagGraphNode(id = "IngestfromFluxinput", label = "Ingest from Flux input"),
            DagGraphNode(id = "TransformPersontoTargetusingVyne", label = "Transform Person to Target using Vyne"),
            DagGraphNode(id = "WritetoListsink", label = "Write to List sink"),
         )
      )

      dataset.links.should.equal(listOf(
         DagGraphLink(source = "IngestfromFluxinput", target = "TransformPersontoTargetusingVyne", label = ""),
         DagGraphLink(source = "TransformPersontoTargetusingVyne", target = "WritetoListsink", label = ""),
      ))
   }
}
