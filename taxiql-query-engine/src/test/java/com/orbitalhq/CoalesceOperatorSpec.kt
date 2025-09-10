package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CoalesceOperatorSpec : DescribeSpec({
   // These are not tests for the coalesce() function in the stdlib.
   describe("Coalesce operator (?:)") {
      val (vyne) = testVyne()
      it("should return correctly for strings") {
         vyne.query(
            """
            given { a: String = "foo", b: String = null , c: String = "bar" }
            find {
               a1 : String = a ?: b
               a2 : String = b ?: a
               a3 : String = c ?: a
               a4 : String = a ?: c
            }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(
               mapOf(
                  "a1" to "foo",
                  "a2" to "foo",
                  "a3" to "bar",
                  "a4" to "foo"
               )
            )
      }
      // This should be implemented, but isn't. There's a bug in the type
      // checker that makes this impossible
      it("should return correctly for arrays") {
         vyne.query(
            """
            given { a: String[] = ["foo"], b: String[] = null , c: String[] = ["bar","baz"] }
            find {
               a1 : String[] = a ?: b
               a2 : String[] = b ?: a
               a3 : String[] = c ?: a
               a4 : String[] = a ?: c
               a5 : String[] = b ?: (String[]) [] // the cast shouldn't be needed
            }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(
               mapOf(
                  "a1" to listOf("foo"),
                  "a2" to listOf("foo"),
                  "a3" to listOf("bar", "baz"),
                  "a4" to listOf("foo"),
                  "a5" to emptyList<String>()
               )
            )
      }
      it("coalescing a projected array when the value is not null returns the array") {
         val (vyne) = testVyne(
            """
            model Address {
               street : Street inherits String
            }
            model Person {
               name : Name inherits String
               addresses : Address[]
             }

         """.trimIndent()
         )
         val result = vyne.query(
            """
            given { Person = {
               name : "Jimmy",
               addresses: [
                 { "street" : "Oxford St" },
                 { "street" : "Regent St" }
               ]
              }
            }
            find { Person } as {
               name: Name
               addr: Address[] as {
                  st : Street
               }[] ?: []
            }
         """.trimIndent()
         )
            .firstRawObject()
         result["addr"].shouldBeInstanceOf<List<*>>()
            .shouldHaveSize(2)
      }

      xit("coalescing a projected array returned from a mutation when the value is not null returns the array") {
         val (vyne,stub) = testVyne("""
             model Address {
               street : Street inherits String
            }
            model Person {
               name : Name inherits String
               addresses : Address[]
               workAddress : Address[]
             }
             service PersonApi {
               write operation getPerson():Person
            }
         """.trimIndent())
         stub.addResponse("getPerson", """
            {
               "name" : "Jimmy",
               "addresses": [
                 { "street" : "Oxford St" },
                 { "street" : "Regent St" }
               ],
               "workAddress": [
                  { "street" : "Tot St" },
                  { "street" : "Bit St" }
               ]
              }
            }
         """.trimIndent())
         val result = vyne.query("""
            call PersonApi::getPerson as {
               name : Name
               addr: Address[] as {
                  st: Street
               }[] ?: []
            }
         """.trimIndent())
            .firstRawObject()
         result
      }
   }
})
