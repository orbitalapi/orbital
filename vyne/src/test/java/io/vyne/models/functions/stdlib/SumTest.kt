package io.vyne.models.functions.stdlib

import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import io.vyne.testVyne
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class SumTest {

   @Test
   fun `can sum an array`() {
      val (vyne, stub) = testVyne(
         """
      model Transaction {
         cost : Cost inherits Int
      }
      model Output {
         transactions : Transaction[]
         total : Int by sum(this.transactions, (Transaction) -> Cost)
      }"""
      )
      val json = """{
         | "transactions" : [ { "cost" : 100 } , { "cost" : 20 } ]
         |}
      """.trimMargin()
      val parsed = vyne.parseJson("Output", json).toRawObject() as Map<String, Any>
      parsed["total"]!!.should.equal(120)
   }
   @Test
   fun `can sum an array using an expression`() {
      val (vyne, stub) = testVyne(
         """
      model Transaction {
         cost : Cost inherits Int
         units: Units inherits Int
      }
      model Output {
         transactions : Transaction[]
         total : Int by sum(this.transactions, (Transaction) -> Cost * Units)
      }"""
      )
      val json = """{
         | "transactions" : [ { "cost" : 100 , "units" : 5 } , { "cost" : 20 , "units" : 200 } ]
         |}
      """.trimMargin()
      val parsed = vyne.parseJson("Output", json).toRawObject() as Map<String, Any>
      parsed["total"]!!.should.equal(4500)
   }
   @Test
   fun `can use sum in an expression`() {
      val (vyne, stub) = testVyne(
         """
      model Transaction {
         cost : Cost inherits Int
         units: Units inherits Int
      }
      model Output {
         transactions : Transaction[]
         total : Decimal by sum(this.transactions, (Transaction) -> Cost) / sum(this.transactions, (Transaction) -> Units)
      }"""
      )
      val json = """{
         | "transactions" : [ { "cost" : 100 , "units" : 5 } , { "cost" : 20 , "units" : 200 } ]
         |}
      """.trimMargin()
      val parsed = vyne.parseJson("Output", json)
      val map = parsed.toRawObject() as Map<String, Any>
      val totalBigDecimal = map["total"]!! as BigDecimal
      totalBigDecimal.setScale(10, RoundingMode.HALF_UP).should.equal("0.5853658537".toBigDecimal())
   }

}
