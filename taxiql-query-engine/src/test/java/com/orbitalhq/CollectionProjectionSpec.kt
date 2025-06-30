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

         // Commented this out, as it's not implemented, and I'm not sure it should be.
         // It makes sense on first pass (null -> null), but I need to understand the
         // broader implications are for (null -> query/discover).
         // ORB-979
         xit("should return null when the source is null, and source is loaded from another lookup") {
            val (vyne,stub) = testVyne("""
            closed model Actor {
               name : Name inherits String
            }
            closed model Film {
               title: Title inherits String
            }
            closed model CastList {
               cast : Actor[]
            }
            model ProjectedCast {
               actorsName : Name
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
            stub.addResponse("getCast", """{ "cast": null }""")
            val result = vyne.query("""find { Film[] } as {
               |  title : Title
               |  cast: CastList as Actor[] as ProjectedCast[]
               |}[]
            """.trimMargin())
               .firstTypedInstace()
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String, Any>>()))
         }


         // Commented this out, as it's not implemented, and I'm not sure it should be.
         // It makes sense on first pass (null -> null), but I need to understand the
         // broader implications are for (null -> query/discover).
         // ORB-979
         xit("should return null when the source is null") {
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
               "cast" : null
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
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to null))
         }
      }

   }
})
