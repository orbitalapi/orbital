package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.jet.Job
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.annotations.streaming.StreamingQueryAnnotations
import com.orbitalhq.pipelines.jet.TestTopic
import com.orbitalhq.pipelines.jet.api.JobStatus
import com.orbitalhq.pipelines.jet.api.PipelineStatus
import com.orbitalhq.pipelines.jet.api.RunningPipelineSummary
import com.orbitalhq.pipelines.jet.api.SubmittedPipeline
import com.orbitalhq.pipelines.jet.api.streams.StreamJobStateEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.query.StreamingQueryInputSpec
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import lang.taxi.query.TaxiQlQuery
import org.glassfish.jaxb.core.v2.TODO
import reactor.core.publisher.Flux
import java.time.Instant

class PersistentStreamManagerTest : DescribeSpec({
   describe("Persistent Stream Manager") {
      it("detects parallel streams") {
         val (store, streamManager, pipelineManager) = storeAndManager()
         val streamState = streamManager.streamStateManager.streamStateCache
         streamState["MyStream"] = StreamStatus("MyStream", StreamStatus.State.PAUSED)
         whenever(pipelineManager.submitStream(any(), any())).thenReturn(mock())

         store.setSchema(
            TaxiSchema.fromStrings(
               StreamingQueryAnnotations.schema,
               """
            import com.orbitalhq.streams.Parallel
            model Foo

            @Parallel(count = 4)
            query MyStream {
               stream { Foo }
            }
         """
            )
         )
         val captor = argumentCaptor<ManagedStream>()
         verify(pipelineManager, times(1)).submitStream(captor.capture(), any())

         captor.lastValue.parallelism.shouldBe(4)
      }

      it("by default streams are created but not started") {
         val (store, streamManager, pipelineManager) = storeAndManager()
         val streamState = streamManager.streamStateManager.streamStateCache
         streamState["MyStream"] = StreamStatus("MyStream", StreamStatus.State.PAUSED)
         whenever(pipelineManager.submitStream(any(), any())).thenReturn(mock())

         store.setSchema(
            TaxiSchema.from(
               """
            model Foo
            query MyStream {
               stream { Foo }
            }
         """
            )
         )
         verify(pipelineManager, times(1)).submitStream(any<ManagedStream>(), any())
         verify(pipelineManager, never()).startPipeline(any())
      }

      it("when a stream is configured to be running, the pipeline manager is triggered to start it") {
         val (store, streamManager, pipelineManager) = storeAndManager()
         val streamState = streamManager.streamStateManager.streamStateCache
         streamState["MyStream"] = StreamStatus("MyStream", StreamStatus.State.RUNNING)
         whenever(pipelineManager.submitStream(any(), any())).thenReturn(mock())

         store.setSchema(
            TaxiSchema.from(
               """
            model Foo
            query MyStream {
               stream { Foo }
            }
         """
            )
         )

         verify(pipelineManager, times(1)).submitStream(any<ManagedStream>(), any())
         verify(pipelineManager, times(1)).startPipeline(anyOrNull())
      }

      it("does not create an entry for queries that are not streams") {
         val (store, manager) = storeAndManager()
         store.setSchema(
            TaxiSchema.from(
               """
            model Foo
            query MyStream {
               find { Foo }
            }
         """
            )
         )

         manager.listCurrentStreams().shouldBeEmpty()
      }

      it("removes the stream when it has been removed") {
         val (store, streamManager, pipelineManager) = storeAndManager()
         val streamState = streamManager.streamStateManager.streamStateCache
         streamState["MyStream"] = StreamStatus("MyStream", StreamStatus.State.RUNNING)
         whenever(pipelineManager.submitStream(any(), any())).thenReturn(mock())

         val querySrc = """ query MyStream {
               stream { Foo }
            }"""
         store.setSchema(
            TaxiSchema.from(
               """
            model Foo
$querySrc
         """
            )
         )

         val argCaptor = argumentCaptor<ManagedStream>()
         verify(pipelineManager, times(1)).submitStream(argCaptor.capture(), any())
         reset(pipelineManager)
          whenever(pipelineManager.canStartPipelines()).thenReturn(true)

         val runningPipeline = RunningPipelineSummary(
            SubmittedPipeline(
               "MyStream",
               "foo-123",
               PipelineSpec("MyStream", StreamingQueryInputSpec(querySrc), null, emptyList()),
               "",
               mock { },
               false
            ),
            PipelineStatus("MyStream", "foo-123", JobStatus.RUNNING, Instant.now(), mock { })
         )
         whenever(pipelineManager.getManagedStreams(false)) doReturn listOf(runningPipeline)
         store.setSchema(TaxiSchema.from("""model Foo"""))
         verify(pipelineManager, times(1)).terminatePipeline(any<String>(), any())

      }

       it("when a stream is configured to be running, but the pipeline manager is not leader then the stream is not triggered to start it") {
           val (store, streamManager, pipelineManager) = storeAndManager()
           reset(pipelineManager)
           whenever(pipelineManager.canStartPipelines()).thenReturn(false)

           val streamState = streamManager.streamStateManager.streamStateCache
           streamState["MyStream"] = StreamStatus("MyStream", StreamStatus.State.RUNNING)
           whenever(pipelineManager.submitStream(any(), any())).thenReturn(mock())

           store.setSchema(
               TaxiSchema.from(
                   """
            model Foo
            query MyStream {
               stream { Foo }
            }
         """
               )
           )

           verify(pipelineManager, times(0)).submitStream(any<ManagedStream>(), any())
           verify(pipelineManager, times(0)).startPipeline(anyOrNull())
       }

       it("when a scheme update is received with same content the pipeline manager is not triggered to restart a running pipeline") {
           val (store, streamManager, pipelineManager) = storeAndManager()
           val streamState = streamManager.streamStateManager.streamStateCache
           streamState["MyStream"] = StreamStatus("MyStream", StreamStatus.State.RUNNING)
           whenever(pipelineManager.submitStream(any(), any())).thenReturn(mock())

           val taxiSchema =  TaxiSchema.from(
               """
            model Foo
            query MyStream {
               stream { Foo }
            }
         """
           )
           store.setSchema(
               taxiSchema
           )

           verify(pipelineManager, times(1)).submitStream(any<ManagedStream>(), any())
           verify(pipelineManager, times(1)).startPipeline(anyOrNull())

           reset(pipelineManager)
           whenever(pipelineManager.canStartPipelines()).thenReturn(true)

           val querySrc = """ query MyStream {
               stream { Foo }
            }"""
           val runningPipeline = RunningPipelineSummary(
               SubmittedPipeline(
                   "MyStream",
                   "foo-123",
                   PipelineSpec("MyStream", StreamingQueryInputSpec(querySrc), null, emptyList()),
                   "",
                   mock { },
                   false
               ),
               PipelineStatus("MyStream", "foo-123", JobStatus.RUNNING, Instant.now(), mock { })
           )
           whenever(pipelineManager.getManagedStreams(false)) doReturn listOf(runningPipeline)
           store.setSchema(taxiSchema)
           verify(pipelineManager, times(0)).submitStream(any<ManagedStream>(), any())
           verify(pipelineManager, times(0)).startPipeline(anyOrNull())
       }
   }
})

private fun storeAndManager(): Triple<SimpleSchemaStore, PersistentStreamManager, PipelineManager> {
   val store = SimpleSchemaStore()
   val pipelineManager: PipelineManager = mock {
      on {canStartPipelines()} doReturn true
      on { jobStatusEvents } doReturn Flux.empty()
   }
   val stateManager = StreamStateManager(StreamStatus.State.PAUSED, pipelineManager, mutableMapOf(), mutableMapOf(), TestTopic())
   val manager = PersistentStreamManager(store, pipelineManager, stateManager)
   return Triple(store, manager, pipelineManager)
}
