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

 @Test //ORB-1060
 fun `spread operator works on nested projected array`(): Unit = runBlocking {
    val (vyne,stub) = testVyne("""
            model Customer {
               id : CustomerId inherits Int
               name : CustomerName inherits String
            }
            model Order {
               orderId : OrderId inherits Int
               items : OrderItem[]
            }
            model OrderItem {
               quantity : Quantity inherits Int
               price : Price inherits Double
            }
            service TestApi {
               operation getCustomer(CustomerId):Customer
               operation getOrdersForCustomer(CustomerId):Order[]
            }
         """)
    stub.addResponse("getCustomer", """{ "id" : 100,  "name" : "Jimmy McJimmerson" }""")
    stub.addResponse("getOrdersForCustomer", """[ { "orderId" : 100, "items" : [ { "quantity" : 3, "price" : 2.0   } ] } ]""")
    val result = vyne.query("""given { CustomerId = 12345 }
find { Customer } as {
    name : CustomerName
    orders: Order[] as {
        name: String = "Jimmy"
        ...
    }[]
}""").firstRawObject()
    result.shouldBe(mapOf(
       "name" to "Jimmy McJimmerson",
       "orders" to listOf(
          mapOf("name" to "Jimmy",
             "orderId" to 100,
             "items" to listOf(
                mapOf("quantity" to 3, "price" to 2.0)
             ))
       )
    ))
 }
}
