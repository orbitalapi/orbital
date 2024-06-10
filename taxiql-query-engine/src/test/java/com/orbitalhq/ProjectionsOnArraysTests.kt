package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.stubbing.StubService
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.MockKGateway
import org.junit.Before
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
            model Film {
               title : FilmTitle inherits String
               cast : CastId inherits Int
            }
            model Actor {
               name : PersonName inherits String
            }
            model FilmCast {
               actors : Actor[]
            }
            service Movies {
               operation findFilm(FilmId):Film
               operation getCast(CastId):FilmCast
            }
         """.trimIndent()
      )
   }

}
