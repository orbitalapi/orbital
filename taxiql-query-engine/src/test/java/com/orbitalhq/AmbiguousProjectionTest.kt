package com.orbitalhq

import com.orbitalhq.models.AmbiguousResult
import com.orbitalhq.models.TypedNull
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf

class AmbiguousProjectionTest : DescribeSpec({
   describe("expressions and projections that are ambiguous") {
      it("should return null when using as to select type") {
         val (vyne,stub) = testVyne("""
            model Person {
              addresses: {
                house: HouseNumber inherits String
                street: StreetName inherits String
              }[]
            }
         """.trimIndent())
         val result = vyne.query("""
            given { person:Person = {
                addresses: [
                    { house: "2", street: "Charles St"},
                    { house: "4", street: "Hurley St"}
                ]
            }}
            find {
                streetName : person as StreetName
            }
         """.trimIndent())
            .firstTypedObject()
         val streetName = result.get("streetName").shouldBeInstanceOf<TypedNull>()
         streetName.source.shouldBeInstanceOf<AmbiguousResult>()
      }
   }
}) {
}
