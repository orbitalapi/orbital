package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.PipelineConfig
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.sink.list.ListSinkSpec
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.fqn
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PipelineLoaderTest : BaseJetIntegrationTest() {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   val jackson = jacksonObjectMapper().registerModule(PipelineJacksonModule())

   @Test
   fun `pipelines are loaded and submitted`() {
      val spec = mapOf(
         "pipeline1.pipeline.json" to PipelineSpec(
            "test-pipeline",
            input = FixedItemsSourceSpec(
               items = queueOf("""{ "firstName" : "jimmy" }"""),
               typeName = "Person".fqn()
            ),
            output = ListSinkSpec("Target")
         ),
         "pipeline2.pipeline.json" to PipelineSpec(
            "test-pipeline",
            input = FixedItemsSourceSpec(
               items = queueOf("""{ "firstName" : "jimmy" }"""),
               typeName = "Person".fqn()
            ),
            output = ListSinkSpec("Target2")
         ),
      )

      spec.forEach { (fileName, spec) ->
         jackson.writeValue(folder.newFile(fileName), spec)
      }

      val manager = mock<PipelineManager> { }
      val loader = PipelineLoader(
         PipelineConfig(folder.root.toPath()),
         jackson,
         manager
      )
      verify(manager, times(2)).startPipeline(any())
   }

   @Test
   fun `when json is invalid then valid loads are completed`() {
      val spec = PipelineSpec(
         "test-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy" }"""),
            typeName = "Person".fqn()
         ),
         output = ListSinkSpec("Target")
      )

      jackson.writeValue(folder.newFile("spec.pipeline.json"), spec)
      folder.newFile("invalid.pipeline.json").writeText("I am broken")
      val manager = mock<PipelineManager> { }
      val loader = PipelineLoader(
         PipelineConfig(folder.root.toPath()),
         jackson,
         manager
      )
      // One file was invalid, but should still submit for the valid file
      verify(manager, times(1)).startPipeline(any())
   }
}
