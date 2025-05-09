package com.orbitalhq.functions.stdlib.math

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class RoundSpec : DescribeSpec({
   describe("stdlib round function") {
      it("should round to a whole number") {
         val (vyne, _) = testVyne("")
         val result = vyne.query(
            """find {
            |wholeNumber : 3.14.round(),
            |onePlace : 3.14.round(1)
            |}
         """.trimMargin()
         )
            .firstRawObject()
         result.shouldBe(mapOf(
            "wholeNumber" to 3.toBigDecimal(),
            "onePlace" to 3.1.toBigDecimal(),
         )
         )
      }
   }
})
