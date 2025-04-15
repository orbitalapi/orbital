package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.stubbing.StubService
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class ProjectionsOnArraysTest {


   //ORB-224
   @Test
   fun `wtf`(): Unit = runBlocking {
      val (vyne, stub) = testVyne()
      stub.addResponse("findFilm", vyne.parseJson("Film", """{ "title" : "Star Wars", "cast" : 100 }"""))
      stub.addResponse(
         "getCast",
         vyne.parseJson(
            "FilmCast",
            """{ "actors" : [{ "name" : "Mark" }] }"""
         )
      )

      val result = vyne.query(
         """
         given { FilmId = 1 }
         find { Film } as {
            actor : first(Actor[]) as {
               firstName : PersonName
            }
         }
      """.trimIndent()
      )
         .firstRawObject()
      result.shouldBe(mapOf("actor" to mapOf("firstName" to "Mark")))

      val result2 = vyne.query(
         """
         given { FilmId = 1 }
         find { Film } as  {
            actor : first(Actor[]) as {
               firstName : PersonName
            }
         }
      """.trimIndent()
      )
         .firstRawObject()
      result2.shouldNotBeNull()
   }

   private fun testVyne(): Pair<Vyne, StubService> {
      return testVyne(
         """
            type FilmId inherits Int
            closed model Film {
               title : FilmTitle inherits String
               cast : CastId inherits Int
            }
            closed model Actor {
               name : PersonName inherits String
            }
            closed model FilmCast {
               actors : Actor[]
            }
            service Movies {
               operation findFilm(FilmId):Film
               operation getCast(CastId):FilmCast
            }
         """.trimIndent()
      )
   }

   @Test // ORB-739
   fun `services returning empty array from array discovery should result in empty array`():Unit = runBlocking {
      val (vyne,stub) = testVyne()
      stub.addResponse("findFilm", """{ "title": "Star Wars", "cast" : 100 }""")
      stub.addResponse("getCast", """{ "actors" : [] }""")
      val result = vyne.query("""
         |given { FilmId = 1 } find { Film } as
         |{
         |  title : FilmTitle,
         |  cast : Actor[]
         |}
      """.trimMargin())
         .firstRawObject()
      result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String,Any>>()))
   }

   @Test // ORB-739
   fun `projecting a discovered empty array should result in empty array`():Unit = runBlocking {
      val (vyne,stub) = testVyne()
      stub.addResponse("findFilm", """{ "title": "Star Wars", "cast" : 100 }""")
      stub.addResponse("getCast", """{ "actors" : [] }""")
      val result = vyne.query("""
         |given { FilmId = 1 }
         |find { Film } as
         |{
         |  title : FilmTitle,
         |  cast : Actor[] as {
         |     firstName : PersonName
         |  }[]
         |}
      """.trimMargin())
         .firstRawObject()
      result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String,Any>>()))
   }

   @Test // ORB-739
   fun `services returning null from array discovery should result in null`():Unit = runBlocking {
      val (vyne,stub) = testVyne()
      stub.addResponse("findFilm", """{ "title": "Star Wars", "cast" : 100 }""")
      stub.addResponse("getCast", """{ "actors" : null }""")
      val result = vyne.query("""given { FilmId = 1 } find { Film } as
         |{
         |  title : FilmTitle,
         |  cast : Actor[]
         |}
      """.trimMargin())
         .firstRawObject()
      result.shouldBe(mapOf("title" to "Star Wars", "cast" to null))
   }

   @Test // ORB-739
   fun `services returning top-level empty array from array discovery should result in empty array`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
            type FilmId inherits Int
            model Film {
               title : FilmTitle inherits String
               cast : CastId inherits Int
            }
            model Actor {
               name : PersonName inherits String
            }
            service Movies {
               operation findFilm(FilmId):Film
               operation getCast(CastId):Actor[]
            }
      """.trimIndent())
      stub.addResponse("findFilm", """{ "title": "Star Wars", "cast" : 100 }""")
      stub.addResponse("getCast", """[]""")
      val result = vyne.query("""given { FilmId = 1 } find { Film } as
         |{
         |  title : FilmTitle,
         |  cast : Actor[]
         |}
      """.trimMargin())
         .firstRawObject()
      result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String,Any>>()))
   }

   // ORB-739
   // This was throwing an error, as adding an empty array to the fact bag, and then
   // searching for it was throwing an exception
   @Test // ORB-739
   fun `parsing empty array`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         type FilmId inherits Int
         closed model Film {
            title : FilmTitle inherits String
            cast : Actor[]
         }
         closed model Actor {
            name : PersonName inherits String
         }
         service Movies {
            operation findFilm():Film
         }
      """.trimIndent())
      stub.addResponse("findFilm", """{ "title": "Star Wars", "cast" : [] }""")
      val result = vyne.query("""
         find { Film } as
         {
           title : FilmTitle
           cast : Actor[] as {
             actorName : PersonName
           }[]
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("title" to "Star Wars", "cast" to emptyList<Map<String,Any>>()))
   }
}
