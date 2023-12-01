package com.orbitalhq.pipelines.jet.streams

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.pipelines.jet.api.JobStatus
import com.orbitalhq.pipelines.jet.api.PipelineStatus
import com.orbitalhq.pipelines.jet.api.RunningPipelineSummary
import com.orbitalhq.pipelines.jet.api.SubmittedPipeline
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.query.StreamingQueryInputSpec
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.time.Instant

class PersistentStreamManagerTest : DescribeSpec({
    describe("Pesistent Stream Manager") {
        it("should create an entry for new streams") {
            val (store, streamManager, pipelineManager) = storeAndManager()
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

            verify(pipelineManager, times(1)).startPipeline(any<ManagedStream>(), any())
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
            verify(pipelineManager, times(1)).startPipeline(argCaptor.capture(), any())
            reset(pipelineManager)

            val runningPipeline = RunningPipelineSummary(
                SubmittedPipeline("MyStream", "foo-123", PipelineSpec("MyStream", StreamingQueryInputSpec(querySrc), null, emptyList()), "", mock { }, false),
                PipelineStatus("MyStream", "foo-123", JobStatus.RUNNING, Instant.now(), mock { })
            )
            whenever(pipelineManager.getManagedStreams(false)) doReturn listOf(runningPipeline)
            store.setSchema(TaxiSchema.from("""model Foo"""))
            verify(pipelineManager, times(1)).terminatePipeline(any<String>(), any())

        }
    }


})

private fun storeAndManager(): Triple<SimpleSchemaStore, PersistentStreamManager, PipelineManager> {
    val store = SimpleSchemaStore()
    val pipelineManager: PipelineManager = mock { }
    val manager = PersistentStreamManager(store, pipelineManager)
    return Triple(store, manager, pipelineManager)
}
