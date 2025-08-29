package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CollectionSizeTest : DescribeSpec({
   describe("stdlib size function") {
      val (vyne, _) = testVyne("")
      it("should return the size of an empty array") {

         vyne.query(
            """
            given { a: String[] = [] }
            find { size: Int =  a.size() }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(mapOf("size" to 0))
      }
      it("should return the size of a non-empty array") {

         vyne.query(
            """
            given { a: String[] = ["a","b","c"] }
            find { size: Int = a.size() }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(mapOf("size" to 3))
      }
      it("should return null as the size of a null array") {

         vyne.query(
            """
            given { a: String[] = null }
            find { size: Int = a.size() }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(mapOf("size" to null))
      }
   }
})
