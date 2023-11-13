package com.orbitalhq.pipelines.jet.streams

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize

class PersistentStreamManagerTest : DescribeSpec({
   describe("Pesistent Stream Manager") {
      it("should create an entry for new streams") {
         val (store, manager) = storeAndManager()
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

         manager.listCurrentStreams().shouldHaveSize(1)
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

      it("updates the stream when it chagnes") {

      }
   }


})

private fun storeAndManager(): Pair<SimpleSchemaStore, PersistentStreamManager> {
   val store = SimpleSchemaStore()
   val manager = PersistentStreamManager(store, mock { })
   return store to manager
}
