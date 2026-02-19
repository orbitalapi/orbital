package com.orbitalhq

import arrow.core.Either
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.right
import com.orbitalhq.models.json.tryParseJson
import com.winterbe.expekt.should
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
         |  },
         |  {
         |  "title" : "Empire Strikes Back",
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
         .rawObjects() as Any
      result.should.equal(
         listOf(
            mapOf(
               "title" to "Star Wars",
               "star" to mapOf(
                  "name" to "Mark Hamill", "title" to "Star Wars"
               )
            ),
            mapOf(
               "title" to "Empire Strikes Back",
               "star" to mapOf(
                  "name" to "Mark Hamill", "title" to "Empire Strikes Back"
               )
            )
         )
      )
   }

   @Test
   fun `a projection can refine whats in scope`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            type FilmId inherits Int
            model Film {
               title : Title inherits String
               cast : Actor[]
            }
            model Actor {
               name : Name inherits String
            }
            service FilmService {
               operation getFilm(FilmId):Film(...)
            }
         """.trimIndent()
      )
      stub.addResponse(
         "getFilm", vyne.parseJson(
            "Film", """{
         "title" : "Star Wars",
          "cast" : [ { "name" : "Mark" } , { "name" : "Carrie" } ]
           }"""
         )
      )
      val queryResult = vyne.query(
         """find { Film(FilmId == 123)  } as Actor[] as {
         | actorName : Name
         | filmTitle : Title // should be null, as it's out-of-scope on Actor
         |}[]
      """.trimMargin()
      )
         .rawObjects()
      queryResult.shouldBe(
         listOf(
            // filmTitle is null, because it's out-of-scope
            mapOf("actorName" to "Mark", "filmTitle" to null),
            mapOf("actorName" to "Carrie", "filmTitle" to null)
         )
      )
   }

   @Test
   fun `can use expressions in projection scopes`() {

   }

   @Test
   fun `expressions in projection scopes can trigger discovery`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            model Film {
               id : FilmId inherits Int
               title : Title inherits String
            }
            model Actor {
               name : ActorName inherits String
            }
            model Cast {
               actors : Actor[]
            }
            service Films {
               operation getFilm(FilmId):Film(...)
               operation getCast(FilmId):Cast
            }
         """.trimIndent()
      )
      stub.addResponse("getFilm", vyne.parseJson("Film", """{ "title" : "Star Wars", "id": 1}"""))
      stub.addResponse(
         "getCast",
         vyne.parseJson("Cast", """{ "actors" : [ { "name" : "Mark" }, {"name" : "Carrie" }]  }""")
      )

      val resultWithoutExplicitScope = vyne.query(
         """
         find { Film(FilmId == 123) } as (first(Actor[])) -> { // note that film has been removed from the scope...
            title : Title //... so we expect this isn't discoverable.
            starring : ActorName
         }
      """.trimIndent()
      )
         .firstRawObject()
      resultWithoutExplicitScope.shouldBe(mapOf("title" to null, "starring" to "Mark"))

      val resultWithExplicitScope = vyne.query(
         """
         find { Film(FilmId == 123) } as (Film, first(Actor[])) -> { // Here, Film is in scope...
            title : Title // .. so this is knowable
            starring : ActorName
         }
      """.trimIndent()
      )
         .firstRawObject()
      resultWithExplicitScope.shouldBe(mapOf("title" to "Star Wars", "starring" to "Mark"))
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
      stub.addResponse("findFilmReview") { _, params ->
         val reviews = vyne.tryParseJson(
            "Review[]",
            """[ { "id" : 1, "filmId": ${params[0].second.value} } ]"""
         )

         when (reviews) {
            is Either.Left -> listOf(Either.Left(reviews.value))
            is Either.Right -> (reviews.value as TypedCollection).map { it.right() }
         }
      }
      val response = vyne.query(
         """find { Film[] } as (src:Film) -> {
         | film : Film
         | review : Review[]( FilmId == src.id )
         |}[]
      """.trimMargin()
      ).rawObjects()
      response.shouldNotBeNull()
   }

   @Test
   fun `can use a named scope to project to a static array type`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            model Film {
               title : Title inherits String
            }
            model Movie {
               name : Title
            }
            model FilmCatalog {
               films : Film[]
            }
            service Films {
               operation getFilms():FilmCatalog
            }
      """.trimIndent()
      )
      stub.addResponse(
         "getFilms", vyne.parseJson(
            "FilmCatalog", """
         { "films" : [ { "title" : "Jaws" }, { "title" : "Star Wars" } ] }
      """.trimIndent()
         )
      )
      val result = vyne.query("""find { FilmCatalog } as Film[] as Movie[]""")
         .rawObjects()
      result.shouldBe(
         listOf(
            mapOf("name" to "Jaws"),
            mapOf("name" to "Star Wars"),
         )
      )
   }

   @Test // ORB-945
   fun `can have multiple named projection scopes in nested projection`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """

         model Application {
             caseId: ApplicationId inherits String
             title : Title inherits String
         }

         model Applicant {
             name : Name inherits String
         }
         service CaseApi {
             operation getCase(ApplicationId):Application
             operation getApplicant(ApplicationId):Applicant
         }
      """.trimIndent()
      )
      stub.addResponsesByParameter(
         "getCase", responses = mapOf(
            "CASE-1" to """{ "caseId" : "CASE-1", "title" : "Mortgage application" }""",
            "CASE-2" to """{ "caseId" : "CASE-2", "title" : "Mortgage application II" }""",
         )
      )
      stub.addResponsesByParameter(
         "getApplicant", responses = mapOf(
            "CASE-1" to """{ "name" : "Jimmy" }""",
            "CASE-2" to """{ "name" : "Jack" }""",
         )
      )
      val result = vyne.query(
         """given { ApplicationId[] = ['CASE-1','CASE-2']}
find { ApplicationId[] } as {
    application: ApplicationId as (theCase: Application, applicant: Applicant) -> {
    caseId: theCase.caseId
    title : theCase.title
    name : applicant.name
  }
}[]"""
      ).rawObjects()
      result.shouldBe(
         listOf(
            mapOf(
               "application" to mapOf(
                  "caseId" to "CASE-1",
                  "title" to "Mortgage application",
                  "name" to "Jimmy"
               )
            ),
            mapOf(
               "application" to mapOf(
                  "caseId" to "CASE-2",
                  "title" to "Mortgage application II",
                  "name" to "Jack"
               )
            )

         )
      )
   }


}
