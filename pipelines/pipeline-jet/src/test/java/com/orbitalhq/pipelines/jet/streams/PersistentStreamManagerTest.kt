package com.orbitalhq.pipelines.jet.streams

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize

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
         reset(pipelineManager)

         store.setSchema(TaxiSchema.from("""model Foo"""))

      }
   }


})

private fun storeAndManager(): Triple<SimpleSchemaStore, PersistentStreamManager, PipelineManager> {
   val store = SimpleSchemaStore()
   val pipelineManager: PipelineManager = mock {  }
   val manager = PersistentStreamManager(store, pipelineManager)
   return Triple(store, manager, pipelineManager)
}
