package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.runBlocking
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

}
