package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import lang.taxi.compiledWithQuery

class NegatedExpressionSpec : DescribeSpec({
   describe("Evaluating negated expressions") {
      it("should evaluate negated expressions") {
         val (vyne, _) = testVyne(
            """
      function alwaysFalse():Boolean -> false
   """.trimIndent()
         )
         val result = vyne.query(
            """ given { b: Boolean = true, a: Boolean = null }
               |find {
               | justB : Boolean = b
               | notB: Boolean = !b
               | notTrue : Boolean = !true
               | notFalse : Boolean = !false
               | withFunction : Boolean = !alwaysFalse()
               | withExpression : Boolean = !(2 == 3)
               | withNull : Boolean? = !a
               |}
            """.trimMargin()
         ).firstRawObject()
         result.shouldBe(mapOf(
            "justB" to true,
            "notB" to false,
            "notTrue" to false,
            "notFalse" to true,
            "withFunction" to true,
            "withExpression" to true,
            "withNull" to null
         ))

      }
   }
})
