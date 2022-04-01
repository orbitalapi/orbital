package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hazelcast.jet.Job
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.pipelines.jet.api.SubmittedPipeline
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
import org.junit.jupiter.api.Assertions.*
import org.junit.rules.TemporaryFolder

class PipelineServiceTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   val jackson = jacksonObjectMapper().registerModule(PipelineJacksonModule())

   @Test
   fun `when a pipeline is submitted it is written to the config`() {
      val pipelineManager: PipelineManager = mock {
         on { startPipeline(any()) } doReturn Pair<SubmittedPipeline, Job>(mock { }, mock { })
      }
      val repository = PipelineRepository(folder.root.toPath(), jackson)
      val service = PipelineService(
         pipelineManager,
         repository
      )

      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         id = "pipeline-1",
         input = GenericPipelineTransportSpec(
            type = "test-input",
            direction = PipelineDirection.INPUT,
         ),
         output = GenericPipelineTransportSpec("test-output", direction = PipelineDirection.OUTPUT)
      )
      service.submitPipeline(
         pipelineSpec
      )

      val writtenPipeline = folder.root.resolve("pipeline-1.pipeline.json")
      writtenPipeline.exists().should.be.`true`

      val readFromDisk = jackson.readValue<PipelineSpec<*, *>>(writtenPipeline)
      readFromDisk.should.equal(pipelineSpec)
   }


}
