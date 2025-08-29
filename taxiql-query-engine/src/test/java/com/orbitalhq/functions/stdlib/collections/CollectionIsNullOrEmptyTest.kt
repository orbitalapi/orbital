package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CollectionIsNullOrEmptyTest : DescribeSpec({
   describe("stdlib isNullOrEmpty") {
      val (vyne) = testVyne("")
      it("should return true for null array") {
         vyne.query(
            """
            given { array: String[] = null }
            find { isNullOrEmpty: Boolean = array.isNullOrEmpty() }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(
               mapOf(
                  "isNullOrEmpty" to true
               )
            )
      }
      it("should return true for empty array") {
         vyne.query(
            """
            given { array: String[] = [] }
            find { isNullOrEmpty: Boolean = array.isNullOrEmpty() }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(
               mapOf(
                  "isNullOrEmpty" to true
               )
            )
      }
      it("should return false for populated array") {
         vyne.query(
            """
            given { array: String[] = ["a","b"] }
            find { isNullOrEmpty: Boolean = array.isNullOrEmpty() }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(
               mapOf(
                  "isNullOrEmpty" to false
               )
            )
      }
   }
})
