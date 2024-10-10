package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class EnumValueSpec : DescribeSpec({
   describe("Evaluating enum values") {
      it("can use a value to look up another value from an enum") {
         val (vyne) = testVyne(
            """
            model ErrorDetails {
               code : ErrorCode inherits Int
               message : ErrorMessage inherits String
            }
            enum Errors<ErrorDetails> {
               BadRequest({ code : 400, message : 'Bad Request' }),
               Unauthorized({ code : 401, message : 'Unauthorized' })
            }
         """.trimIndent()
         )
         val result = vyne.query(
            """
            given { errorResponse : String = 'BadRequest' }
            find {
               error : ErrorDetails by Errors.enumForName(errorResponse)
            }
         """.trimIndent()
         )
            .firstTypedObject()
         val raw = result.toRawObject()
         raw.shouldBe(
            mapOf(
               "error" to mapOf("code" to 400, "message" to "Bad Request")
            )
         )
      }

      it("will use a default value if the named lookup fails") {
         val (vyne) = testVyne(
            """
      model ErrorDetails {
         code : ErrorCode inherits Int
         message : ErrorMessage inherits String
      }
      enum Errors<ErrorDetails> {
         default BadRequest({ code : 400, message : 'Bad Request' }),
         Unauthorized({ code : 401, message : 'Unauthorized' })
      }
   """.trimIndent()
         )
         val result = vyne.query(
            """
      given { errorResponse : String = 'Poopsy' }
      find {
         error : ErrorDetails by Errors.enumForName(errorResponse)
      }
   """.trimIndent()
         )
            .firstRawObject()
         result shouldNotBe null
         result.shouldBe(
            mapOf(
               "error" to mapOf("code" to 400, "message" to "Bad Request")
            )
         )
      }

      describe("querying with enum object") {
         val schema = """
         model ErrorDetails {
            code : ErrorCode inherits Int
            message : ErrorMessage inherits String
         }
         enum Errors<ErrorDetails> {
            BadRequest({ code : 400, message : 'Bad Request' }),
            Unauthorized({ code : 401, message : 'Unauthorized' })
         }
         """.trimIndent()
         it("can do a find on an enum as top-level object") {
            val (vyne) = testVyne(schema)
            val result = vyne.query(
               """
find {  Errors.enumForName('BadRequest') }
            """.trimIndent()
            )
               .firstTypedInstace().toRawObject()
            result.shouldBe(
               mapOf("code" to 400, "message" to "Bad Request")
            )
         }
         it("can use an enum as a top-level field") {
            val (vyne) = testVyne(schema)
            val result = vyne.query(
               """
find {
    errorCode: Errors by Errors.enumForName('BadRequest')
}
            """.trimIndent()
            )
               .firstRawObject()
            result.shouldBe(
               mapOf(
                  "errorCode" to mapOf("code" to 400, "message" to "Bad Request")
               )
            )
         }

         it("can use a enum object value as a projection source with top-level inline projection") {
            val (vyne) = testVyne(schema)
            val result = vyne.query(
               """
            given { errorResponse : String = 'BadRequest' }
            find {
               error : Errors by Errors.enumForName(errorResponse) as (errorDetails: ErrorDetails) -> {
                  errorCode : ErrorCode
                  errorMessage : ErrorMessage
               }
            }
         """.trimIndent()
            )
               .firstTypedObject()
            val raw = result.toRawObject()
            raw.shouldBe(
               mapOf(
                  "error" to mapOf("errorCode" to 400, "errorMessage" to "Bad Request")
               )
            )
         }

         it("can use a enum object value as a projection source on a body projection") {
            val (vyne) = testVyne(schema)
            val result = vyne.query(
               """
            given { errorResponse : String = 'BadRequest' }
            find { "Hello" } as {
               error : ErrorDetails by Errors.enumForName(errorResponse)  as (errorDetails: ErrorDetails) -> {
                  errorCode : ErrorCode
                  errorMessage : ErrorMessage
               }
            }
         """.trimIndent()
            )
               .firstTypedObject()
            val raw = result.toRawObject()
            raw.shouldBe(
               mapOf(
                  "error" to mapOf("errorCode" to 400, "errorMessage" to "Bad Request")
               )
            )
         }
      }


   }
})
