package com.orbitalhq

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Test

class FormattedTypesTest {

   @Test // ORB-759
   fun `formats applied in projection are honoured`() :Unit = runBlocking{
      val (vyne,stub) = testVyne("""
         model StockReport {
            quantity : Quantity inherits Int
            @Format("dd/MM/yyyy")
            lastUpdated : LastUpdated inherits Date
         }
         service StockApi {
            operation getStock():StockReport
         }
      """.trimIndent())
      stub.addResponse("getStock", """{ "quantity" : 200, "lastUpdated" : "10/05/2023" }""")
      val result = vyne.query("""find { StockReport } as {
         | @Format("MM-yy")
         | lastUpdated: LastUpdated
         |}
      """.trimMargin())
         .firstTypedInstace()
      result.toRawObject()
         .shouldBe(mapOf("lastUpdated" to "05-23"))
   }

   @Test // ORB-759
   fun `formats applied in projected array are honoured`() :Unit = runBlocking{
      val (vyne,stub) = testVyne("""
         model StockReport {
            quantity : Quantity inherits Int
            @Format("dd/MM/yyyy")
            lastUpdated : LastUpdated inherits Date
         }
         service StockApi {
            operation getStock():StockReport[]
         }
      """.trimIndent())
      stub.addResponse("getStock", """[{ "quantity" : 200, "lastUpdated" : "10/05/2023" }]""")
      val result = vyne.query("""find { StockReport[] } as {
         | @Format("MM-yy")
         | lastUpdated: LastUpdated
         |}[]
      """.trimMargin())
         .firstTypedInstace()
      result.toRawObject()
         .shouldBe(mapOf("lastUpdated" to "05-23"))
   }

   @Test // ORB-759
   fun `formats applied in projected array under top level object are honoured`() :Unit = runBlocking{
      val (vyne,stub) = testVyne("""
         model StockReport {
            quantity : Quantity inherits Int
            @Format("dd/MM/yyyy")
            lastUpdated : LastUpdated inherits Date
         }
         service StockApi {
            operation getStock():StockReport[]
         }
      """.trimIndent())
      stub.addResponse("getStock", """[{ "quantity" : 200, "lastUpdated" : "10/05/2023" }]""")
      val result = vyne.query("""find {
         |stockReport: StockReport[] as {
         |   @Format("MM-yy")
         |   lastUpdated: LastUpdated
         |  }[]
         |}
      """.trimMargin())
         .firstTypedInstace()
      result.toRawObject()
         .shouldBe(mapOf("stockReport" to listOf(mapOf("lastUpdated" to "05-23"))))
   }
}
