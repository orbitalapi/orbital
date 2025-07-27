package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AppendSpec : DescribeSpec({
   describe("Append") {
      // ORB-992
      it("should combine members") {
         val (vyne,_) = testVyne("""
            model Person {
               name : Name inherits String
            }
         """.trimIndent())
         val result = vyne.query("""
            given {
               some : Person[] = [{name: "Jim"}],
               other: Person = { name : "Jack"}
            }
            find {
               people : Person[] = some.append([other])
            }
         """.trimIndent())
            .firstRawObject()
         result.shouldBe(mapOf("people" to listOf(
            mapOf("name" to "Jim"),
            mapOf("name" to "Jack"),
         )))
      }
   }
})
