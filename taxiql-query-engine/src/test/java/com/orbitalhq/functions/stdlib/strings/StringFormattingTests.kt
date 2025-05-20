package com.orbitalhq.functions.stdlib.strings

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StringFormattingTests : DescribeSpec({
   describe("padStart and padEnd") {
      it("should pad left and right correctly") {
         val (vyne, _) = testVyne("")
         val result = vyne.query("""
            find {
               start: 'foo'.padStart(8,'-')
               end : 'foo'.padEnd(8,'-')
               tooLongStart: 'alreadyTooLong'.padStart(2,'-')
               tooLongEnd: 'alreadyTooLong'.padEnd(2,'-')
            }
         """.trimIndent())
            .firstRawObject()

         result.shouldBe(mapOf(
            "start" to "-----foo",
            "end" to "foo-----",
            "tooLongStart" to "alreadyTooLong",
            "tooLongEnd" to "alreadyTooLong",
         ))
      }

      it("returns null if padded string is null") {
         val (vyne, _) = testVyne("")
         val result = vyne.query("""
            given { source: String = null }
            find {
               start: source.padStart(8,'-')
               end : source.padEnd(8,'-')
            }
         """.trimIndent())
            .firstRawObject()

         result.shouldBe(mapOf(
            "start" to null,
            "end" to null
         ))
      }
   }

   describe("applyFormat") {
      it("should apply the provided format") {
         val (vyne,stub) = testVyne("")
         val result = vyne.query("""
            given { balance: Decimal = -6217.58 }
            find {
               formatted: String = balance.applyFormat("Amount gained or lost since last statement: ${'$'} %(,.2f")
            }
         """.trimIndent())
            .firstRawObject()
         result.shouldBe(mapOf(
            "formatted" to "Amount gained or lost since last statement: \$ (6,217.58)"
         ))
      }
   }

})
