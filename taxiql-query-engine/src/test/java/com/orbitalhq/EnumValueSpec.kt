package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class EnumValueSpec : DescribeSpec({
   describe("Evaluating enum values") {
      it("can use a value to look up another value from an enum") {
         val (vyne) = testVyne("""
            model ErrorDetails {
               code : ErrorCode inherits Int
               message : ErrorMessage inherits String
            }
            enum Errors<ErrorDetails> {
               BadRequest({ code : 400, message : 'Bad Request' }),
               Unauthorized({ code : 401, message : 'Unauthorized' })
            }
         """.trimIndent())
         val result = vyne.query("""
            given { errorResponse : String = 'BadRequest' }
            find {
               error : ErrorDetails by Errors.enumForName(errorResponse)
            }
         """.trimIndent())
            .firstTypedObject()
         val raw = result.toRawObject()
         raw.shouldBe(mapOf(
            "error" to mapOf("code" to 400, "message" to "Bad Request")
         ))
      }

      it("will use a default value if the named lookup fails") {
         val (vyne) = testVyne("""
      model ErrorDetails {
         code : ErrorCode inherits Int
         message : ErrorMessage inherits String
      }
      enum Errors<ErrorDetails> {
         default BadRequest({ code : 400, message : 'Bad Request' }),
         Unauthorized({ code : 401, message : 'Unauthorized' })
      }
   """.trimIndent())
         val result = vyne.query("""
      given { errorResponse : String = 'Poopsy' }
      find {
         error : ErrorDetails by Errors.enumForName(errorResponse)
      }
   """.trimIndent())
            .firstRawObject()
         result shouldNotBe null
         result.shouldBe(mapOf(
            "error" to mapOf("code" to 400, "message" to "Bad Request")
         ))
      }
   }
})
