package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CollectionJoinToStringSpec : DescribeSpec({
   describe("joinToString") {
      val schema = TaxiSchema.from(
         """
       closed model Person {
           name : PersonName inherits String
          }
   """.trimIndent()
      )


     it("joins on a property") {
        val (vyne,stub) = testVyne(schema)
        vyne.query("""
           given { s:String[] = ['a','b','c'] }
           find { result : String[].joinToString() }
        """.trimIndent())
           .firstRawObject()
           .shouldBe(mapOf("result" to "a,b,c"))
     }

      it("joins on a property with a prefix and postfix") {
         val (vyne,stub) = testVyne(schema)
         vyne.query("""
           given { s:String[] = ['a','b','c'] }
           find { result : String[].joinToString(" | ","start:"," end.") }
        """.trimIndent())
            .firstRawObject()
            .shouldBe(mapOf("result" to "start:a | b | c end."))
      }

      it("joins numbers and handles a null value") {
         val (vyne,stub) = testVyne(schema)
         vyne.query("""
           given { s:Int[] = [1,null,2] }
           find { result : Int[].joinToString() }
        """.trimIndent())
            .firstRawObject()
            .shouldBe(mapOf("result" to "1,2"))
      }

      it("can join non-scalar values using a map function") {
         val (vyne,stub) = testVyne(schema)
         vyne.query("""
           given { people:Person[] = [{ name: "Jimmy" }, { name: "Jack" }] }
           find { result : Person[].map((Person) -> PersonName).joinToString() }
        """.trimIndent())
            .firstRawObject()
            .shouldBe(mapOf("result" to "Jimmy,Jack"))
      }

      it("can join non-scalar values using a projection") {
         val (vyne,stub) = testVyne(schema)
         vyne.query("""
           given { people:Person[] = [{ name: "Jimmy" }, { name: "Jack" }] }
           find { result : (Person[] as PersonName[]).joinToString() }
        """.trimIndent())
            .firstRawObject()
            .shouldBe(mapOf("result" to "Jimmy,Jack"))
      }
   }
})
