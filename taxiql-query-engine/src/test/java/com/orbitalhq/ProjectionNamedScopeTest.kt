package com.orbitalhq

import com.winterbe.expekt.should
import io.kotest.matchers.nulls.shouldNotBeNull
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

class ProjectionNamedScopeTest {

   @Test
   fun `can project using a named scope at the top level`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Actor {
              actorId : ActorId inherits Int
              name : ActorName inherits String
            }
            model Film {
               title : FilmTitle inherits String
               headliner : ActorId
               cast: Actor[]
            }
            service DataService {
               operation getFilms():Film[]
            }
      """.trimIndent()
      )
      stub.addResponse(
         "getFilms", vyne.parseJson(
            "Film[]", """[
         |{
         |  "title" : "Star Wars",
         |  "headliner" : 1 ,
         |  "cast": [
         |     { "actorId" : 1 , "name" : "Mark Hamill" },
         |     { "actorId" : 2 , "name" : "Carrie Fisher" }
         |     ]
         |  }
         |]
      """.trimMargin()
         )
      )
      val result = vyne.query(
         """find { Film[] } as (film:Film) -> {
         |  movieName : film.title
         |}[]
      """.trimMargin()
      )
         .firstRawObject()
      result.should.equal(mapOf("movieName" to "Star Wars"))
   }

   @Test
   fun `can project using a mix of different named scopes`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Actor {
              actorId : ActorId inherits Int
              name : ActorName inherits String
            }
            model Film {
               title : FilmTitle inherits String
               headliner : ActorId
               cast: Actor[]
            }
            service DataService {
               operation getFilms():Film[]
            }
      """.trimIndent()
      )
      stub.addResponse(
         "getFilms", vyne.parseJson(
            "Film[]", """[
         |{
         |  "title" : "Star Wars",
         |  "headliner" : 1 ,
         |  "cast": [
         |     { "actorId" : 1 , "name" : "Mark Hamill" },
         |     { "actorId" : 2 , "name" : "Carrie Fisher" }
         |     ]
         |  }
         |]
      """.trimMargin()
         )
      )
      val result = vyne.query(
         """
         find { Film[] } as (film:Film) -> {
               title : FilmTitle
               star : singleBy(film.cast, (Actor) -> Actor::ActorId, film.headliner) as (actor:Actor) -> {
                  name : actor.name
                  title : film.title
               }
            }[]
      """.trimMargin()
      )
         .firstRawObject()
      result.should.equal(
         mapOf(
            "title" to "Star Wars",
            "star" to mapOf(
               "name" to "Mark Hamill", "title" to "Star Wars"
            )
         )
      )
   }

   @Test
   @Ignore("Doesn't work - ORB-75")
   fun `can use a named scope to refine a service call`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
            title : Title inherits String
         }
         model Review {
            id : ReviewId inherits Int
            filmId : FilmId
         }
         service MyService {
            operation findFilms():Film[]
            // Should not be called
            operation findReviews():Review[]
            operation findFilmReview(id: FilmId):Review[](FilmId == id)
         }
      """.trimIndent()
      )
      stub.addResponse(
         "findFilms", vyne.parseJson(
            "Film[]", """[
         | { "id" : 1, "title" : "Foo" },
         | { "id" : 2, "title" : "Bar" }
         |]
      """.trimMargin()
         )
      )
      stub.addResponse("findFilmReview") { request, params ->
         vyne.parseJson(
            "Review[]",
            """[ { "id" : 1, "filmId": ${params.get(0).second.value} } ]"""
         ) as List<TypedInstance>
      }
      val response = vyne.query(
         """find { Film[] } as (src:Film) -> {
         | film : Film
         | review : Review[]( FilmId == src.id )
         |}[]
      """.trimMargin()
      ).rawObjects()
      response.shouldNotBeNull()
      val invocations = stub.invocations["findFilmReview"]
   }

}