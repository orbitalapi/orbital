package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CollectionProjectionSpec : DescribeSpec({
   describe("Projecting collections") {
      describe("empty and null collections") {
         it("should return empty array when the source is empty") {
            val (vyne, stub) = testVyne(
               """
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
         """.trimIndent()
            )
            stub.addResponse(
               "getFilms", """
            [{
               "title" : "Star Wars",
               "cast" : []
            }]
         """.trimIndent()
            )
            val result = vyne.query(
               """find { Film[] } as {
               |  title : Title
               |  cast: {
               |     actorsName : Name
               |  }[]
               |}[]
            """.trimMargin()
            )
               .firstRawObject()
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String, Any>>()))
         }

         it("should return empty array when the source is empty, and source is loaded from another lookup") {
            val (vyne, stub) = testVyne(
               """
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
         """.trimIndent()
            )
            stub.addResponse(
               "getFilms", """
            [{
               "title" : "Star Wars"
            }]
         """.trimIndent()
            )
            stub.addResponse("getCast", """{ "cast": [] }""")
            val result = vyne.query(
               """find { Film[] } as {
               |  title : Title
               |  cast: CastList as Actor[] as {
               |     actorsName : Name
               |  }[]
               |}[]
            """.trimMargin()
            )
               .firstRawObject()
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String, Any>>()))
         }

         // ORB-979
         it("should return null when the source is null, and source is loaded from another lookup") {
            val (vyne, stub) = testVyne(
               """
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
         """.trimIndent()
            )
            stub.addResponse(
               "getFilms", """
            [{
               "title" : "Star Wars"
            }]
         """.trimIndent()
            )
            stub.addResponse("getCast", """{ "cast": null }""")
            val result = vyne.query(
               """find { Film[] } as {
               |  title : Title
               |  cast: CastList as Actor[] as ProjectedCast[]
               |}[]
            """.trimMargin()
            )
               .firstRawObject()

            // This is the current behaviour, but not neccessarily the right behaviour
            // It's arguable we should be returning null here.
            // MP: 17-Jul -- updated the test. As alluded to, the previous behaviour of returning an empty collection,
            // whilst "nicer", is inconsistent, and breaks expected behaviours with things like coalesce
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to null))
         }


         // ORB-979
         it("if the source is not explicit, but is null, then use an empty list") {
            val (vyne, stub) = testVyne(
               """
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
         """.trimIndent()
            )
            stub.addResponse(
               "getFilms", """
            [{
               "title" : "Star Wars",
               "cast" : null
            }]
         """.trimIndent()
            )
            val result = vyne.query(
               """find { Film[] } as {
               |  title : Title
               |  // Should this be null, or an empty list?
               |  // We're not explicit about how to populate, so an argument could be made that an empty list is
               |  // a better response?
               |  cast: {
               |     actorsName : Name
               |  }[]
               |}[]
            """.trimMargin()
            )
               .firstRawObject()
            // MP: Given there was no source defined, it's a discovery, rather than projecting null.
            // A discovery can return an empty list, so the value becomes [], rather than null
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String, Any>>()))
         }

         // ORB-979
         it("should return null when the source is explicitly defined and is null") {
            val (vyne, stub) = testVyne(
               """
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
         """.trimIndent()
            )
            stub.addResponse(
               "getFilms", """
            [{
               "title" : "Star Wars",
               "cast" : null
            }]
         """.trimIndent()
            )
            val result = vyne.query(
               """find { Film[] } as {
               |  title : Title
               |
               |  // Here we're explicit about projecting Actor[], which is returned as null,
               |   // so the result should be null
               |  cast: Actor[] as {
               |     actorsName : Name
               |  }[]
               |}[]
            """.trimMargin()
            )
               .firstRawObject()
            // MP: 17-Jul-25
            // This behaviour changed, as previously we returned an empty list here.
            // However, the consistent behaviour is null projects to null,
            // so should be null.
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to null))
         }

         it("should return empty list when the source is explicitly defined and is null, but the projection is coalesced to an empty list") {
            val (vyne, stub) = testVyne(
               """
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
         """.trimIndent()
            )
            stub.addResponse(
               "getFilms", """
            [{
               "title" : "Star Wars",
               "cast" : null
            }]
         """.trimIndent()
            )
            val result = vyne.query(
               """find { Film[] } as {
               |  title : Title
               |
               |  // Here we're explicit about projecting Actor[], which is returned as null,
               |   // so the result should be null
               |  cast: Actor[] as {
               |     actorsName : Name
               |  }[] ?: [] // Note that here we coalesce to an empty list, so the result should be an empty list
               |}[]
            """.trimMargin()
            )
               .firstRawObject()
            // This is the current behaviour, but not neccessarily the right behaviour.
            // Tracking this so we have coverage of what happens currently - anything else
            // is a regression, and needs to be messaged to users, but
            // arguably we should be returning null here.
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Any>()))
         }

         it("should return empty list when the source is explicitly defined and is null, but the input is coalesced to an empty list") {
            val (vyne, stub) = testVyne(
               """
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
         """.trimIndent()
            )
            stub.addResponse(
               "getFilms", """
            [{
               "title" : "Star Wars",
               "cast" : null
            }]
         """.trimIndent()
            )
            val result = vyne.query(
               """find { Film[] } as {
               |  title : Title
               |
               |  // Here we're explicit about projecting Actor[], which is returned as null,
               |   // so the result should be null -- but is then coalesced into an empty list, so the result
               |   // becomes an empty list
               |  cast: (Actor[] ?: []) as {
               |     actorsName : Name
               |  }[]
               |}[]
            """.trimMargin()
            )
               .firstRawObject()
            // This is the current behaviour, but not neccessarily the right behaviour.
            // Tracking this so we have coverage of what happens currently - anything else
            // is a regression, and needs to be messaged to users, but
            // arguably we should be returning null here.
            result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Any>()))
         }
      }
   }

})
