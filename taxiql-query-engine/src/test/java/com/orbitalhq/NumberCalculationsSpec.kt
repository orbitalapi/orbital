package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.math.RoundingMode

class NumberCalculationsSpec : DescribeSpec({
   describe("calculating simple expressions") {
      val (vyne,_) = testVyne("")
      it("should handle division correctly") {
         vyne.queryForMap("""
            find {
               a: Decimal = 4 / 10
               b: Decimal = ((4 / 250) * 100).round(1)
               c: String = 'A' + 'B'
              // Division resulting in repeating decimals
              d: Decimal = (1 / 3).round(5)
              e: Decimal = (2 / 3).round(5)

              // Division that looks like it might be whole numbers
              f: Decimal = 10 / 5
              g: Decimal = 100 / 4

              // Division with larger numbers
              h: Decimal = (1000 / 333).round(10)

              // Division by 1 (should still be decimal)
              i: Decimal = 42 / 1
            }
         """)
         .shouldBe(mapOf(
            "a" to 0.4.toBigDecimal(),
            "b" to 1.6.toBigDecimal(),
            "c" to "AB",
            "d" to (1.toBigDecimal().divide(3.toBigDecimal(), 5, RoundingMode.HALF_UP)),
            "e" to (2.toBigDecimal().divide(3.toBigDecimal(),5, RoundingMode.HALF_UP)),
            "f" to 2.toBigDecimal(),
            "g" to 25.toBigDecimal(),
            "h" to (1000.toBigDecimal().divide(333.toBigDecimal(),10, RoundingMode.HALF_UP)),
            "i" to 42.toBigDecimal()
         ))

      }
      it("should handle addition and subtraction with division") {
         vyne.queryForMap("""
    find {
        // Addition after division
        j: Decimal = (1 / 2) + 5
        k: Decimal = 5 + (1 / 2)

        // Subtraction after division
        l: Decimal = (3 / 4) - 1
        m: Decimal = 1 - (3 / 4)

        // Multiple operations
        n: Decimal = (1 / 2) + (1 / 4)
        o: Decimal = (10 / 3).round(2) - (5 / 6).round(4)
    }
""")
            .shouldBe(mapOf(
               "j" to 5.5.toBigDecimal(),
               "k" to 5.5.toBigDecimal(),
               "l" to (-0.25).toBigDecimal(),
               "m" to 0.25.toBigDecimal(),
               "n" to 0.75.toBigDecimal(),
               "o" to 10.toBigDecimal().divide(3.toBigDecimal(), 2, RoundingMode.HALF_UP).subtract(5.toBigDecimal().divide(6.toBigDecimal(), 4, RoundingMode.HALF_UP))
            ))
      }
      it("handle nested operations") {
         vyne.queryForMap("""
    find {
        // Nested parentheses with mixed operations
        p: Decimal = ((10 / 4) * 2) + 1
        q: Decimal = (100 / (5 * 4)) * 8

        // Multiple divisions and multiplications
        r: Decimal = (8 / 2) / 4
        s: Decimal = 8 / (2 / 4)

        // Operations with zero
        t: Decimal = 0 / 5
        u: Decimal = (5 / 10) * 0
    }
""")
            .shouldBe(mapOf(
               "p" to 6.0.toBigDecimal(),
               "q" to 40.toBigDecimal(),
               "r" to 1.toBigDecimal(),
               "s" to 16.toBigDecimal(),
               "t" to 0.toBigDecimal(),
               "u" to 0.0.toBigDecimal()
            ))

      }
   }
})
private suspend fun Vyne.queryForMap(query: String):Map<String,Any?> {
   val result = this.query(query)
      .firstRawObject()
   return result
}
