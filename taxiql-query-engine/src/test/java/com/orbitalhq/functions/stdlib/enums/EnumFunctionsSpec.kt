package com.orbitalhq.functions.stdlib.enums

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EnumFunctionsSpec : DescribeSpec({

   describe("enum functions") {
      val (vyne, _) = testVyne("""
               model ErrorResponse {
                  message: ErrorMessage inherits String
                  code : ResponseCode inherits Int
               }
               enum HttpErrors<ErrorResponse> {
                  NOT_FOUND({message: 'Nope, not here', code: 404}),
                  OK({message: 'Nice', code: 200})
               }
            """.trimIndent())
      context("enum for name") {
         it("returns an enum looked up by name") {
            vyne.query("""
               |given { name : String = 'NOT_FOUND' }
               |find {
               | responseCode: HttpErrors.enumForName(name).code
               |}
            """.trimMargin())
               .firstRawObject().shouldBe(mapOf("responseCode" to 404))
         }
         it("returns null if the looked up value is not present") {
            vyne.query("""
               |given { name : String = 'NOPEY_WOPEY' }
               |find {
               | responseCode: HttpErrors.enumForName(name).code
               |}
            """.trimMargin())
               .firstRawObject().shouldBe(mapOf("responseCode" to null))
         }

         it("returns null if the input is null") {
            vyne.query("""
               |given { name : String = null }
               |find {
               | responseCode: HttpErrors.enumForName(name).code
               |}
            """.trimMargin())
               .firstRawObject().shouldBe(mapOf("responseCode" to null))
         }
      }

      context("has enum named") {
         it("returns correct values when looking up by name") {
            vyne.query("""
               |given { nullString:String = null }
               |find {
               | hasOK: Boolean = HttpErrors.hasEnumNamed('OK')
               | hasNope: Boolean = HttpErrors.hasEnumNamed('Nope')
               | hasNull : Boolean = HttpErrors.hasEnumNamed(nullString)
               |}
            """.trimMargin())
               .firstRawObject().shouldBe(mapOf("hasOK" to true, "hasNope" to false, "hasNull" to null))
         }
      }
   }
})
