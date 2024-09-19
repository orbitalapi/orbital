package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ExpressionTypeSpec : DescribeSpec({

   describe("Expression types") {
      it("evaluates when clause on an expression type correctly") {
         val (vyne) = testVyne(
            """
      type CustomerType inherits String
      type AccountType inherits String by when(lowerCase(CustomerType)) {
           'retail'  -> 'Personal'
           'sme' -> 'Personal'
           else -> 'Business'
      }
   """
         )
         vyne.query("""
         given { CustomerType = 'Retail' }
         find { AccountType }
      """.trimIndent())
            .firstRawValue().shouldBe("Personal")
      }
   }
})
