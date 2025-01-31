package com.orbitalhq

import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import lang.taxi.asA
import lang.taxi.compiledWithQuery
import lang.taxi.types.ObjectType
import org.junit.jupiter.api.Test

// See also: VyneProjectionTest
class SpreadOperatorTest {

   @Test
   fun `can use spread operator excepts clause in top-level find`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type Message inherits String
         model StockQuote {
            ticker : Ticker inherits String
            price : Price inherits Decimal
            quantity : Quantity inherits Int
         }
         service StockApi {
            operation getQuote():StockQuote
         }
      """.trimIndent()
      )
      stub.addResponse("getQuote", """{  "ticker" : "Xyz", "price": 200.30, "quantity" : 1000 } """)
      val result = vyne.query(
         """
         find { quote: StockQuote as { ... except { quantity } } }
      """.trimIndent()
      ).firstRawObject()
      result.shouldBe(mapOf("quote" to mapOf("ticker" to "Xyz", "price" to 200.30.toBigDecimal())))
   }

   @Test
   fun `can use spread operator excepts clause on inherited type in top-level find`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type Message inherits String
         model StockQuote {
            ticker : Ticker inherits String
            price : Price inherits Decimal
            quantity : Quantity inherits Int
         }
         model BuySideQuote inherits StockQuote
         service StockApi {
            operation getQuote():BuySideQuote
         }
      """.trimIndent()
      )
      stub.addResponse("getQuote", """{  "ticker" : "Xyz", "price": 200.30, "quantity" : 1000 } """)
      val result = vyne.query(
         """
         find { quote: BuySideQuote as { ... except { quantity } } }
      """.trimIndent()
      ).firstRawObject()
      result.shouldBe(mapOf("quote" to mapOf("ticker" to "Xyz", "price" to 200.30.toBigDecimal())))
   }
}
