package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CollectionProjectionSpec : DescribeSpec({
   describe("Projecting collections") {
      describe("empty and null collections") {
         it("should return empty array when the source is empty") {
            val (vyne,stub) = testVyne("""
            model Actor {
               name : Name inherits String
            }
            model Film {
               title: Title inherits String
               cast: Actor[]
            }
            service FilmApi {
               operation getFilms():Film[]
            }
         """.trimIndent())
            stub.addResponse("getFilms","""
            [{
               "title" : "Star Wars",
               "cast" : []
            }]
         """.trimIndent())
            val result = vyne.query("""find { Film[] } as {
               |  title : Title
               |  cast: {
               |     actorsName : Name
               |  }[]
               |}[]
            """.trimMargin())
               .firstRawObject()
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String, Any>>()))
         }

         it("should return empty array when the source is empty, and source is loaded from another lookup") {
            val (vyne,stub) = testVyne("""
            model Actor {
               name : Name inherits String
            }
            model Film {
               title: Title inherits String
            }
            model CastList {
               cast : Actor[]
            }
            service FilmApi {
               operation getFilms():Film[]
               operation getCast():CastList
            }
         """.trimIndent())
            stub.addResponse("getFilms","""
            [{
               "title" : "Star Wars"
            }]
         """.trimIndent())
            stub.addResponse("getCast", """{ "cast": [] }""")
            val result = vyne.query("""find { Film[] } as {
               |  title : Title
               |  cast: CastList as Actor[] as {
               |     actorsName : Name
               |  }[]
               |}[]
            """.trimMargin())
               .firstRawObject()
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String, Any>>()))
         }

      }

   }
})
