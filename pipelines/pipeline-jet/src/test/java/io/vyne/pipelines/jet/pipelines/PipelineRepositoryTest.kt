package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
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

class PipelineRepositoryTest : BaseJetIntegrationTest() {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   val jackson = jacksonObjectMapper().registerModule(PipelineJacksonModule())

   @Test
   fun `pipelines are loaded from disk`() {
      val specs = mapOf(
         "pipeline1.pipeline.json" to PipelineSpec(
            "test-pipeline",
            id = "pipeline-1",
            input = FixedItemsSourceSpec(
               items = queueOf("""{ "firstName" : "jimmy" }"""),
               typeName = "Person".fqn()
            ),
            output = ListSinkSpec("Target")
         ),
         "pipeline2.pipeline.json" to PipelineSpec(
            "test-pipeline",
            id = "pipeline-2",
            input = FixedItemsSourceSpec(
               items = queueOf("""{ "firstName" : "jimmy" }"""),
               typeName = "Person".fqn()
            ),
            output = ListSinkSpec("Target2")
         ),
      )


      specs.forEach { (fileName, spec) ->
         jackson.writeValue(folder.newFile(fileName), spec)
      }

      val manager = mock<PipelineManager> { }
      val loader = PipelineRepository(
         folder.root.toPath(),
         jackson,
      )
      val loadedPipelines = loader.loadPipelines()
      loadedPipelines.should.have.size(2)
      val existingSpecs = specs.values.toList()
      loadedPipelines.should.contain.elements(existingSpecs[0], existingSpecs[1])
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
      val loader = PipelineRepository(
         folder.root.toPath(),
         jackson,
      )
      // We should return one file, since one is invalid.
      loader.loadPipelines().should.have.size(1)
   }
}
