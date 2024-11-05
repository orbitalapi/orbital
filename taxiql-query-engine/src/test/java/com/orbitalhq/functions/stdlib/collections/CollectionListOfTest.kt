package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.Test

class CollectionListOfTest : DescribeSpec({

   describe("listOf()") {
      // THis doesn't work, as the generic resolution fails.
      // Fixing that is a job for another day.
      xit("can use listOf to return empty list") {
         val (vyne) = testVyne(
            """
            model Film {
               title : Title inherits String
            }
         """.trimIndent()
         )
         vyne.query(
            """
            given { film: Film = { title : "Jaws" } }
            find {
               films : Film[] = listOf()
            }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(mapOf("films" to emptyList<Map<String,Any>>()))

      }
      it("returns a list populated with provided values with single element") {
         val (vyne) = testVyne(
            """
            model Film {
               title : Title inherits String
            }
         """.trimIndent()
         )
         vyne.query(
            """
            given { film: Film = { title : "Jaws" } }
            find {
               films : Film[] = listOf(film)
            }
         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(mapOf("films" to listOf(mapOf("title" to "Jaws"))))

      }

      it("returns a list populated with provided values with multiple elements") {
         val (vyne) = testVyne(
            """
            model Film {
               title : Title inherits String
            }
         """.trimIndent()
         )
         vyne.query(
            """
            given { film1: Film = { title : "Jaws" }, film2: Film = { title : "Jaws 2" } }
            find {
               films : Film[] = listOf(film1, film2)
            }

         """.trimIndent()
         )
            .firstRawObject()
            .shouldBe(
               mapOf(
                  "films" to listOf(
                     mapOf("title" to "Jaws"),
                     mapOf("title" to "Jaws 2"),

                     )
               )
            )

      }
   }
})
