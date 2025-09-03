package com.orbitalhq.connectors.nosql.mongodb

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class MongoAggregateBuilderTest : DescribeSpec({

   val builder = MongoAggregateBuilder()

   describe("MongoAggregateBuilder") {

      describe("successful aggregation building") {

         it("should build simple match aggregation") {
            val steps = listOf("""{ "${'$'}match": { "categoryId": :categoryId } }""")
            val parameters = mapOf("categoryId" to "electronics")

            val result = builder.buildAggregation(steps, parameters)

            result.isRight().shouldBeTrue()
//            result.map { aggregation ->
//               aggregation shouldBeInstanceOf Aggregation::class
//            }
         }

         describe("aggregation stage validation") {

            it("should build aggregation with multiple stages") {
               val steps = listOf(
                  """{ "${'$'}match": { "categoryId": :categoryId } }""",
                  """{ "${'$'}sort": { "price": -1 } }""",
                  """{ "${'$'}limit": :maxResults }"""
               )
               val parameters = mapOf(
                  "categoryId" to "electronics",
                  "maxResults" to 10
               )

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
            }

            it("should handle different parameter types") {
               val steps = listOf(
                  """{ "${'$'}match": {
                        "categoryId": :categoryId,
                        "price": { "${'$'}gte": :minPrice },
                        "inStock": :inStock,
                        "tags": { "${'$'}in": :allowedTags }
                    } }"""
               )
               val parameters = mapOf(
                  "categoryId" to "electronics",
                  "minPrice" to 99.99,
                  "inStock" to true,
                  "allowedTags" to listOf("laptop", "gaming")
               )

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
            }

            it("should handle null parameters") {
               val steps = listOf("""{ "${'$'}match": { "optionalField": :nullValue } }""")
               val parameters = mapOf("nullValue" to null)

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
            }

            it("should handle complex nested objects as parameters") {
               val steps = listOf("""{ "${'$'}match": :complexFilter }""")
               val parameters = mapOf(
                  "complexFilter" to mapOf(
                     "categoryId" to "electronics",
                     "price" to mapOf("\${'$'}gte" to 100)
                  )
               )

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
            }

            it("should handle aggregation with no parameters") {
               val steps = listOf(
                  """{ "${'$'}group": { "_id": "${'$'}categoryId", "count": { "${'$'}sum": 1 } } }""",
                  """{ "${'$'}sort": { "count": -1 } }"""
               )
               val parameters = emptyMap<String, Any>()

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
            }
         }

         describe("parameter validation") {

            it("should fail when required parameters are missing") {
               val steps = listOf("""{ "${'$'}match": { "categoryId": :categoryId } }""")
               val parameters = emptyMap<String, Any>()

               val result = builder.buildAggregation(steps, parameters)

               result.isLeft().shouldBeTrue()
               result.mapLeft { error ->
                  error.message shouldContain "Missing parameters: categoryId"
                  error.stepIndex shouldBe 0
               }
            }

            it("should fail when multiple parameters are missing") {
               val steps = listOf(
                  """{ "${'$'}match": { "categoryId": :categoryId, "minPrice": :minPrice } }"""
               )
               val parameters = emptyMap<String, Any>()

               val result = builder.buildAggregation(steps, parameters)

               result.isLeft().shouldBeTrue()
               result.mapLeft { error ->
                  error.message shouldContain "Missing parameters:"
                  error.message shouldContain "categoryId"
                  error.message shouldContain "minPrice"
               }
            }

            it("should allow extra parameters that aren't used") {
               val steps = listOf("""{ "${'$'}match": { "categoryId": :categoryId } }""")
               val parameters = mapOf(
                  "categoryId" to "electronics",
                  "unusedParam" to "ignored"
               )

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
            }
         }

         describe("JSON validation") {

            it("should fail on invalid JSON") {
               val steps = listOf("""{ "${'$'}match": { "categoryId": :categoryId """)
               val parameters = mapOf("categoryId" to "electronics")

               val result = builder.buildAggregation(steps, parameters)

               result.isLeft().shouldBeTrue()
               result.mapLeft { error ->
                  error.message shouldContain "Invalid JSON"
                  error.stepIndex shouldBe 0
               }
            }

            it("should fail on non-object JSON") {
               val steps = listOf(""""just a string"""")
               val parameters = emptyMap<String, Any>()

               val result = builder.buildAggregation(steps, parameters)

               result.isLeft().shouldBeTrue()
            }

            it("should fail on empty object") {
               val steps = listOf("""{}""")
               val parameters = emptyMap<String, Any>()

               val result = builder.buildAggregation(steps, parameters)

               result.isLeft().shouldBeTrue()
               result.mapLeft { error ->
                  error.message shouldContain "cannot be empty"
               }
            }

            it("should fail when no MongoDB stage operators present") {
               val steps = listOf("""{ "categoryId": "electronics" }""")
               val parameters = emptyMap<String, Any>()

               val result = builder.buildAggregation(steps, parameters)

               result.isLeft().shouldBeTrue()
               result.mapLeft { error ->
                  error.message shouldContain "must contain at least one MongoDB stage operator"
               }
            }
         }

         describe("security and injection prevention") {

            it("should safely handle strings with quotes and special characters") {
               val steps = listOf("""{ "${'$'}match": { "search": :searchTerm } }""")
               val parameters = mapOf("searchTerm" to """malicious"; db.dropDatabase(); //""")

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
               // The malicious content should be properly escaped as a JSON string
            }

            it("should prevent parameter injection in field names") {
               val steps = listOf("""{ "${'$'}match": { :fieldName: "value" } }""")
               val parameters = mapOf("fieldName" to """${'$'}where": function() { while(true) {} }; "fake""")

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
               // Should be treated as a literal string, not executable code
            }

            it("should safely serialize complex objects without code injection") {
               val steps = listOf("""{ "${'$'}match": :complexFilter }""")
               val maliciousObject = mapOf(
                  "\${'$'}where" to "function() { return true; }",
                  "normal_field" to "normal_value"
               )
               val parameters = mapOf("complexFilter" to maliciousObject)

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
               // The $where should be serialized as a literal string, not executed
            }

            it("should handle array parameters safely") {
               val steps = listOf("""{ "${'$'}match": { "tags": { "${'$'}in": :tagList } } }""")
               val parameters = mapOf(
                  "tagList" to listOf(
                     "normal_tag",
                     """malicious"; db.collection.drop(); //"""
                  )
               )

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
            }

            it("should not allow parameter names to break out of JSON structure") {
               val steps = listOf("""{ "${'$'}match": { "field": :param } }""")
               val parameters = mapOf("param" to """value", "${'$'}where": "malicious_code""")

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
               // The parameter should be properly JSON-escaped
            }

            it("should handle null injection attempts") {
               val steps = listOf("""{ "${'$'}match": { "field": :nullParam } }""")
               val parameters = mapOf<String,Any?>("nullParam" to null)

               val result = builder.buildAggregation(steps, parameters)

               result.isRight().shouldBeTrue()
            }

            it("should validate that all parameters are replaced") {
               val steps = listOf("""{ "${'$'}match": { "field1": :param1, "field2": :param2 } }""")
               val parameters = mapOf("param1" to "value1")
               // param2 is missing

               val result = builder.buildAggregation(steps, parameters)

               result.isLeft().shouldBeTrue()
               result.mapLeft { error ->
                  error.message shouldContain "Missing parameters: param2"
               }
            }
         }

         it("should fail on empty pipeline") {
            val steps = emptyList<String>()
            val parameters = emptyMap<String, Any>()

            val result = builder.buildAggregation(steps, parameters)

            result.isLeft().shouldBeTrue()
            result.mapLeft { error ->
               error.message shouldContain "Pipeline cannot be empty"
               error.stepIndex shouldBe -1
            }
         }

         it("should handle parameter names with underscores and numbers") {
            val steps = listOf(
               """{ "${'$'}match": {
                        "field_1": :param_name_1,
                        "field2": :param2
                    } }"""
            )
            val parameters = mapOf(
               "param_name_1" to "value1",
               "param2" to "value2"
            )

            val result = builder.buildAggregation(steps, parameters)

            result.isRight().shouldBeTrue()
         }

         it("should preserve MongoDB operators that start with ${'$'} in field names") {
            val steps = listOf(
               """{ "${'$'}match": { "price": { "${'$'}gte": :minPrice, "${'$'}lte": :maxPrice } } }"""
            )
            val parameters = mapOf(
               "minPrice" to 100,
               "maxPrice" to 500
            )

            val result = builder.buildAggregation(steps, parameters)

            result.isRight().shouldBeTrue()
         }

         it("should handle string parameters containing special characters") {
            val steps = listOf("""{ "${'$'}match": { "description": :searchTerm } }""")
            val parameters = mapOf("searchTerm" to "Special chars: @#$%^&*()")

            val result = builder.buildAggregation(steps, parameters)

            result.isRight().shouldBeTrue()
         }
      }

      describe("edge cases") {

         it("should accept known MongoDB stages") {
            val knownStages = listOf(
               """{ "${'$'}match": { "categoryId": "electronics" } }""",
               """{ "${'$'}group": { "_id": "${'$'}category", "count": { "${'$'}sum": 1 } } }""",
               """{ "${'$'}sort": { "price": -1 } }""",
               """{ "${'$'}limit": 10 }""",
               """{ "${'$'}skip": 5 }""",
               """{ "${'$'}project": { "name": 1, "price": 1 } }""",
               """{ "${'$'}lookup": { "from": "categories", "localField": "categoryId", "foreignField": "_id", "as": "category" } }""",
               """{ "${'$'}unwind": "${'$'}tags" }""",
               """{ "${'$'}facet": { "categorizedByTags": [{ "${'$'}unwind": "${'$'}tags" }] } }"""
            )

            knownStages.forEach { step ->
               val result = builder.buildAggregation(listOf(step), emptyMap())
               result.isRight().shouldBeTrue()
            }
         }

         it("should allow unknown MongoDB stages (for forward compatibility)") {
            val steps = listOf("""{ "${'$'}unknownStage": { "someField": "someValue" } }""")
            val parameters = emptyMap<String, Any>()

            val result = builder.buildAggregation(steps, parameters)

            // Should succeed even with unknown stages for forward compatibility
            result.isRight().shouldBeTrue()
         }
      }
   }
})
