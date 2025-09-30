package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CollectionIndexOfTest : DescribeSpec({
  describe("stdlib indexof") {
     it("should return null if the input is null") {
        val (vyne) = testVyne()
        vyne.query("""
         given {
            names : String[] = null
         }
         find { idx: Int = names.indexOfItem('Jim') }
        """.trimIndent())
           .firstRawObject()
           .shouldBe(mapOf("idx" to null))
     }
     it("should return -1 if not found") {
        val (vyne) = testVyne()
        vyne.query("""
         given {
            names : String[] = ['Jim','Jack','John']
         }
         find { idx: Int = names.indexOfItem('Jimmy') }
        """.trimIndent())
           .firstRawObject()
           .shouldBe(mapOf("idx" to -1))
     }
     it("should return the index if found") {
        val (vyne) = testVyne()
        vyne.query("""
         given {
            names : String[] = ['Jim','Jack','John']
         }
         find { idx: Int = names.indexOfItem('Jim') }
        """.trimIndent())
           .firstRawObject()
           .shouldBe(mapOf("idx" to 0))
     }
  }
})
