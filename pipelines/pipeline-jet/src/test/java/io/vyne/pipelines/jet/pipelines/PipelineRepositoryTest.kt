package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.GenericPipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineDirection
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
            input = GenericPipelineTransportSpec(
               type = "input-1",
               direction = PipelineDirection.INPUT
            ),
            outputs = listOf(
               GenericPipelineTransportSpec(
                  type = "output-1",
                  direction = PipelineDirection.OUTPUT
               )
            )
         ),
         "pipeline2.pipeline.json" to  PipelineSpec(
            "test-pipeline",
            id = "pipeline-2",
            input = GenericPipelineTransportSpec(
               type = "input-1",
               direction = PipelineDirection.INPUT
            ),
            outputs = listOf(
               GenericPipelineTransportSpec(
                  type = "output-1",
                  direction = PipelineDirection.OUTPUT
               )
            )
         ),
      )

      specs.forEach { (fileName, spec) ->
         jackson.writeValue(folder.newFile(fileName), spec)
      }

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
   fun `pipeline ids need to be unique`() {
      val specs = mapOf(
         "pipeline1.pipeline.json" to PipelineSpec(
            "test-pipeline",
            id = "pipeline-1",
            input = GenericPipelineTransportSpec(
               type = "input-1",
               direction = PipelineDirection.INPUT
            ),
            outputs = listOf(
               GenericPipelineTransportSpec(
                  type = "output-1",
                  direction = PipelineDirection.OUTPUT
               )
            )
         ),
         "pipeline2.pipeline.json" to PipelineSpec(
            "test-pipeline",
            id = "pipeline-1",
            input = GenericPipelineTransportSpec(
               type = "input-1",
               direction = PipelineDirection.INPUT
            ),
            outputs = listOf(
               GenericPipelineTransportSpec(
                  type = "output-1",
                  direction = PipelineDirection.OUTPUT
               )
            )
         ),
      )

      specs.forEach { (fileName, spec) ->
         jackson.writeValue(folder.newFile(fileName), spec)
      }

      val loader = PipelineRepository(
         folder.root.toPath(),
         jackson,
      )
      try {
         loader.loadPipelines()
         throw AssertionError("Non-unique pipeline ids should throw an exception")
      } catch (e: IllegalStateException) {
         // Expected to happen
      }
   }

   @Test
   fun `when json is invalid then valid loads are completed`() {
      val spec = PipelineSpec(
         "test-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(ListSinkSpec("Target"))
      )

      jackson.writeValue(folder.newFile("spec.pipeline.json"), spec)
      folder.newFile("invalid.pipeline.json").writeText("I am broken")
      val loader = PipelineRepository(
         folder.root.toPath(),
         jackson,
      )
      // We should return one file, since one is invalid.
      loader.loadPipelines().should.have.size(1)
   }

   @Test
   fun `reads hocon specs`() {
      val configFile = folder.newFile("hocon.conf")
      Resources.copy(Resources.getResource("hocon-pipeline-spec.conf"), configFile.outputStream())
      val loader = PipelineRepository(
         folder.root.toPath(),
         jackson,
      )
      loader.loadPipelines().should.have.size(1)
   }
}
