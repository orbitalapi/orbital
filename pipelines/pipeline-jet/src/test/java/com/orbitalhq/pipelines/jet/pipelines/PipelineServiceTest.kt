package com.orbitalhq.pipelines.jet.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.*
import com.orbitalhq.ParsedSource
import com.orbitalhq.VersionedSource
import com.orbitalhq.asParsedPackage
import com.orbitalhq.asParsedPackages
import com.orbitalhq.pipelines.jet.api.transport.GenericPipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineJacksonModule
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.SchemaSetChangedEvent
import org.awaitility.Awaitility
import org.junit.Ignore
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
   @Ignore("need to fix")
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
      val repository: PipelineConfigRepository = mock {
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
      val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(oldSource).asParsedPackage()), 1)
      val newSchemaSet =
         SchemaSet.fromParsed(listOf(ParsedSource(oldSource), ParsedSource(newSource)).asParsedPackages(), 2)
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


}
