package com.orbitalhq.models.functions.stdlib.collections

import com.orbitalhq.auth.authentication.vyneUserFromClaims
import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class OrEmptySpec : DescribeSpec({
   describe("orEmpty()") {
      it("should return the source array when not null") {
         val (vyne) = testVyne("")
         vyne.query("""
            given { f: String[] = ["A"] }
            find { result : String[] = f.orEmpty() }
         """.trimIndent())
            .firstRawObject()
            .shouldBe(mapOf("result" to listOf("A")))
      }
      it("should return empty on a null input") {
         val (vyne) = testVyne("")
         val result = vyne.query("""
            given { f: String[] = null }
            find { result : String[] = f.orEmpty() }
         """.trimIndent())
            .firstTypedObject()
         result.toRawObject()
            .shouldBe(mapOf("result" to emptyList<Any>()))
         result["result"].type.name.shortDisplayName.shouldBe("String[]")
      }

      it("should return do strings") {
         val (vyne) = testVyne("")
         vyne.query("""
            given { f: String = "A" }
            find { result : f.upperCase() }
         """.trimIndent())
            .firstRawObject()
            .shouldBe(mapOf("result" to emptyList<Any>()))
      }
   }
})
