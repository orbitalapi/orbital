package com.orbitalhq.functions.stdlib.math

import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedObject
import com.orbitalhq.models.FailedEvaluatedExpression
import com.orbitalhq.models.TypedNull
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.math.BigDecimal

class AverageSpec : DescribeSpec({
   describe("Average function") {
      val (vyne, _) = testVyne("")

      describe("Basic functionality") {
         it("should return average of integers") {
            val result = vyne.query("""
               find {
                  intAverage: Decimal = [2,3,4].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("intAverage" to 3.0.toBigDecimal()))
         }

         it("should return average of decimals") {
            val result = vyne.query("""
               find {
                  decimalAverage: Decimal = [2.5, 3.5, 4.0].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("decimalAverage" to BigDecimal("3.3")))
         }


      }

      describe("Edge cases") {
         it("should handle single element collection") {
            val result = vyne.query("""
               find {
                  singleAverage: Decimal = [42].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("singleAverage" to 42.0.toBigDecimal()))
         }

         it("should handle collection with negative numbers") {
            val result = vyne.query("""
               find {
                  negativeAverage: Decimal = [-5, -3, -1].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("negativeAverage" to (-3.0).toBigDecimal()))
         }

         it("should handle collection with zeros") {
            val result = vyne.query("""
               find {
                  zeroAverage: Decimal = [0, 0, 6].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("zeroAverage" to 2.0.toBigDecimal()))
         }

         it("should handle collection with all zeros") {
            val result = vyne.query("""
               find {
                  allZeroAverage: Decimal = [0, 0, 0].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("allZeroAverage" to 0.0.toBigDecimal()))
         }

         it("should handle large numbers") {
            val result = vyne.query("""
               find {
                  largeAverage: Decimal = [999999999, 1000000000, 1000000001].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("largeAverage" to 1000000000.0.toBigDecimal()))
         }

         it("should handle precision with BigDecimal") {
            val result = vyne.query("""
               find {
                  precisionAverage: Decimal = [1.0, 2.0, 3.0].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("precisionAverage" to 2.0.toBigDecimal()))
         }
      }

      describe("Error handling") {
         it("should return error if averaging strings") {
            val result = vyne.query("""find { avg: ["A", "B", "C"].average() } """)
               .firstTypedObject()
            val avg = result["avg"]
            avg.shouldBeInstanceOf<TypedNull>()
            avg.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
               .errorMessage.shouldBe("Cannot average this collection, as it contains the following non-numeric types: String")
         }

         it("should return error for empty collection") {
            val result = vyne.query("""find { avg: [].average() } """)
               .firstTypedObject()
            val avg = result["avg"]
            avg.shouldBeInstanceOf<TypedNull>()
            avg.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
               .errorMessage.shouldBe("Cannot average an empty collection")
         }

         it("should return error for mixed string and numeric types") {
            val result = vyne.query("""find { avg: [1, "2", 3].average() } """)
               .firstTypedObject()
            val avg = result["avg"]
            avg.shouldBeInstanceOf<TypedNull>()
            avg.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
               .errorMessage.shouldBe("Cannot average this collection, as it contains the following non-numeric types: String")
         }

         it("should return error for boolean values") {
            val result = vyne.query("""find { avg: [true, false, true].average() } """)
               .firstTypedObject()
            val avg = result["avg"]
            avg.shouldBeInstanceOf<TypedNull>()
            avg.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
               .errorMessage.shouldBe("Cannot average this collection, as it contains the following non-numeric types: Boolean")
         }

         it("should return error for mixed boolean and numeric types") {
            val result = vyne.query("""find { avg: [1, true, 3].average() } """)
               .firstTypedObject()
            val avg = result["avg"]
            avg.shouldBeInstanceOf<TypedNull>()
            avg.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
               .errorMessage.shouldBe("Cannot average this collection, as it contains the following non-numeric types: Boolean")
         }

         it("should return error for mixed primitive types") {
            val result = vyne.query("""find { avg: [1, 2.0].average() } """)
               .firstTypedObject()
            val avg = result["avg"]
            avg.shouldBeInstanceOf<TypedNull>()
            avg.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
               .errorMessage.shouldBe("Cannot average a collection where the primitive types are mixed -- found lang.taxi.Int, lang.taxi.Decimal")
         }

         it("should return error for null values in collection") {
            val result = vyne.query("""find { avg: [1, null, 3].average() } """)
               .firstTypedObject()
            val avg = result["avg"]
            // This should either filter out nulls or return an error
            // Based on the implementation, it filters nulls but we should test the behavior
            avg.shouldBeInstanceOf<TypedNull>()
         }

         it("should return error for collection with all nulls") {
            val result = vyne.query("""find { avg: [null, null, null].average() } """)
               .firstTypedObject()
            val avg = result["avg"]
            avg.shouldBeInstanceOf<TypedNull>()
            // Should fail because after filtering nulls, the collection is empty
            avg.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
         }
      }

      describe("Complex scenarios") {
         it("should handle decimal precision correctly") {
            val result = vyne.query("""
               find {
                  precisionTest: Decimal = [1.111111111, 2.222222222, 3.333333333].average()
               }
            """.trimIndent())
               .firstRawObject()
            // Should test that precision is maintained appropriately
            result["precisionTest"].shouldBeInstanceOf<BigDecimal>()
         }

         it("should handle alternating positive and negative numbers") {
            val result = vyne.query("""
               find {
                  alternatingAverage: Decimal = [1, -1, 2, -2, 3, -3].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("alternatingAverage" to 0.0.toBigDecimal()))
         }

         it("should handle very small decimal numbers") {
            val result = vyne.query("""
               find {
                  smallDecimalAverage: Decimal = [0.0001, 0.0002, 0.0003].average()
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("smallDecimalAverage" to BigDecimal("0.0002")))
         }
      }

      describe("Type consistency") {
         it("should maintain type consistency for integer inputs") {
            val result = vyne.query("""
               find {
                  intResult: Decimal = [1, 2, 3, 4, 5].average()
               }
            """.trimIndent())
               .firstRawObject()
            result["intResult"].shouldBeInstanceOf<BigDecimal>()
         }

         it("should maintain type consistency for decimal inputs") {
            val result = vyne.query("""
               find {
                  decimalResult: Decimal = [1.1, 2.2, 3.3].average()
               }
            """.trimIndent())
               .firstRawObject()
            result["decimalResult"].shouldBeInstanceOf<BigDecimal>()
         }

         it("should handle type conversion correctly") {
            val result = vyne.query("""
               find {
                  convertedResult: Decimal = [1, 2, 3].average()
               }
            """.trimIndent())
               .firstRawObject()
            // Result should be converted to the expected return type
            result["convertedResult"].shouldBeInstanceOf<BigDecimal>()
         }
      }
   }
})
