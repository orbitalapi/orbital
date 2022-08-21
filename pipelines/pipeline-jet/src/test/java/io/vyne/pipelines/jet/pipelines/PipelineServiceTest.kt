package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hazelcast.jet.Job
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.GenericPipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.SchemaSetChangedEvent
import org.awaitility.Awaitility
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito
import org.reactivestreams.FlowAdapters
import java.util.concurrent.SubmissionPublisher
import java.util.concurrent.TimeUnit


class PipelineServiceTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   val jackson = jacksonObjectMapper().registerModule(PipelineJacksonModule())

   @Test
   fun `when a schema change happens, the remaining pipelines will be submitted`() {
      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         id = "pipeline-1",
         input = GenericPipelineTransportSpec(
            type = "test-input",
            direction = PipelineDirection.INPUT,
            requiredSchemaTypes = listOf("Client")
         ),
         outputs = listOf(GenericPipelineTransportSpec("test-output", direction = PipelineDirection.OUTPUT))
      )

      val pipelineManager: PipelineManager = mock {
         on { startPipeline(any()) } doReturn Pair(mock { }, mock { })
      }
      val repository: PipelineRepository = mock {
         on { loadPipelines() } doReturn listOf(pipelineSpec)
      }
      val publisher = SubmissionPublisher<SchemaSetChangedEvent>()
      val schemaStore: SchemaStore = mock {
         whenever(it.schemaChanged).thenReturn(FlowAdapters.toPublisher(publisher))
      }

      val pipelineService = PipelineService(
         pipelineManager,
         repository,
         schemaStore
      )

      pipelineService.loadAndSubmitExistingPipelines()

      verify(pipelineManager, Mockito.times(0)).startPipeline(any())

      val oldSource = VersionedSource("order.taxi", "1.0.0", "type Order {}")
      val newSource = VersionedSource("client.taxi", "1.1.1", "type Client {}")
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(oldSource)), 1)
      val newSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(oldSource), ParsedSource(newSource)), 2)
      val event = SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)
      publisher.submit(event)

      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         try {
            verify(pipelineManager, Mockito.times(1)).startPipeline(any())
            true
         } catch (e: Throwable) {
            false
         }
      }
   }

   @Test
   fun `when a pipeline is submitted it is written to the config`() {
      val pipelineManager: PipelineManager = mock {
         on { startPipeline(any()) } doReturn Pair<SubmittedPipeline, Job>(mock { }, mock { })
      }
      val repository = PipelineRepository(folder.root.toPath(), jackson)
      val publisher = SubmissionPublisher<SchemaSetChangedEvent>()
      val schemaStore: SchemaStore = mock {
         whenever(it.schemaChanged).thenReturn(FlowAdapters.toPublisher(publisher))
      }
      val service = PipelineService(
         pipelineManager,
         repository,
         schemaStore
      )

      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         id = "pipeline-1",
         input = GenericPipelineTransportSpec(
            type = "test-input",
            direction = PipelineDirection.INPUT,
         ),
         outputs = listOf(GenericPipelineTransportSpec("test-output", direction = PipelineDirection.OUTPUT))
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
