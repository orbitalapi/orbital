package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * An aggregating projection is where we have a source of an array, and
 * it's projection emits a single item.
 *
 * eg:
 * find { AccountLimits[] } as {
 *    totalLimit : AccountLimits[].sum( (AccountLimit) -> AccountLimitAmount )
 * }
 *
 * This should emit a single result, not many.
 */
class ProjectingAggregationSpec : DescribeSpec({

   describe("projections that are aggregations") {
      it("emits a single item containing the correct result") {
         val (vyne, stub) = testVyne(
            """
         type MetroLimitId inherits String
         type AdvisedAmount inherits Decimal

         closed parameter model LimitRead {
             limitId: MetroLimitId
             advisedAmount: AdvisedAmount
         }

         service limitsMongoService {
             operation getLimits() : LimitRead[]
         }
      """.trimIndent()
         )
         stub.addResponse(
            "getLimits", """
         [
         {
            "limitId":"10001771.0011000.01",
            "advisedAmount":5,
            "availableMarker":"Y",
            "expiryDate": "2099-12-31T00:00:00.000"
         },
         {
            "limitId":"10001771.0011000.01",
            "advisedAmount":6,
            "availableMarker":"Y",
            "expiryDate": "2099-12-31T00:00:00.000"
         }
         ]
      """.trimIndent()
         )
         val result = vyne.query(
            """
find {
    LimitRead[]
} as  {
    sum : LimitRead[].sum((LimitRead) -> AdvisedAmount)
}"""
         ).typedObjects() // don't use firstTypedXxx(), as we want to ensure a total of 1 items was emitted
         result.shouldNotBeNull()
         result.shouldHaveSize(1)
         result.single().toRawObject()
            .shouldBe(mapOf(
               "sum" to 11.toBigDecimal()
            ))
      }
   }
}) {
}
