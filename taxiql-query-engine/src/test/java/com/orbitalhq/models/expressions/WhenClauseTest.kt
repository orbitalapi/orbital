package com.orbitalhq.models.expressions

import com.orbitalhq.firstTypedObject
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Test

// See also ConditionalFieldDefinitionTests, ConditionalFieldReaderTEst
class WhenClauseTest {

   @Test
   fun `captures lineage data for an evaluated when clause`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         model StockLevel {
            count : StockCount inherits Int
            reorderLevel : ReorderLevel inherits Int
         }
         type RemainingBeforeReorder = StockCount - ReorderLevel
         service StockLevelsApi {
            operation getStockLevel():StockLevel
         }
      """.trimIndent())

      val query = """
         find { StockLevel } as {
            reorderStatus: String = when {
               RemainingBeforeReorder > 0 && RemainingBeforeReorder < 5 -> "Reorder soon"
               RemainingBeforeReorder < 0 -> "Time to reorder"
               else -> "Sufficient Stock"
            }
         }
      """.trimIndent()
      stub.addResponse("getStockLevel", """{ "count" : 12, "reorderLevel" : 10 }""")
      val reorderSoonResult = vyne.query(query)
         .firstTypedObject()
      reorderSoonResult.toRawObject().shouldBe(mapOf("reorderStatus" to "Reorder soon"))

      stub.clearAll()
      stub.addResponse("getStockLevel", """{ "count" : 5, "reorderLevel" : 10 }""")
      val reorderRequiredResult = vyne.query(query)
         .firstTypedObject()
      reorderRequiredResult.toRawObject().shouldBe(mapOf("reorderStatus" to "Time to reorder"))
      // TODO : Assert data source

      stub.clearAll()
      stub.addResponse("getStockLevel", """{ "count" : 50, "reorderLevel" : 10 }""")
      val sufficientStockResult = vyne.query(query)
         .firstTypedObject()
      sufficientStockResult.toRawObject().shouldBe(mapOf("reorderStatus" to "Sufficient Stock"))
      // TODO : Assert data source

   }
}
