package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.TypedObject
import io.vyne.models.json.parseJson
import io.vyne.utils.asA
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CollectionProjectionTest {
   val schema =
      """
         model FilmReview {}
         model ImdbFilmReview inherits FilmReview {
           id : FilmId inherits Int
           score : FilmScore inherits Decimal
         }
         model RottenTomatoesFilmReview inherits FilmReview {
           id : FilmId
           score : FilmScore
         }
         model Film {
            id : FilmId
            title : FilmTitle inherits String
          }

          model DataResponse {
             films : Film[]
             tomatoReviews:RottenTomatoesFilmReview[]
             imdbReviews:ImdbFilmReview[]
          }

          service DataService {
             operation loadData():DataResponse
          }
      """.trimIndent()
   val responseJson = """{
         | "films" : [
         |  { "id" : 1 , "title" : "Back to the Future" },
         |  { "id" : 2 , "title" : "A New Hope" },
         |  { "id" : 3 , "title" : "Empire Strikes Back" }
         |],
         |"tomatoReviews" : [
         |  { "id" : 1 , "score" : 5.1 },
         |  { "id" : 2 , "score" : 5.2 },
         |  { "id" : 3 , "score" : 5.3 }
         |],
         |"imdbReviews" : [
         |  { "id" : 1 , "score" : 4.1 },
         |  { "id" : 2 , "score" : 4.2 },
         |  { "id" : 3 , "score" : 4.3 }
         |]
         |}
      """.trimMargin()


   @Test
   fun `can project values from source type`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("loadData", vyne.parseJson("DataResponse", responseJson))
      val results = vyne.query(
         """find { DataResponse } as {
         movies : Film[] as {
            id : FilmId
            title : FilmTitle
         }[]
      }
      """
      )
         .typedObjects()
//      val results = vyne.query("""find { DataResponse } as {
//         | films : Film[] as {
//         |      id : FilmId
//         |      title : FilmTitle
//         |      reviews : filterAll(FilmReview[], (FilmReview) -> FilmId == this.id)
//         | }[]
//         |}
//      """.trimMargin())
//         .typedObjects()
      results.should.have.size(1)
      val moviesCollection = results.single().asA<TypedObject>().get("movies") as TypedCollection
      moviesCollection.toRawObject().should.equal(
         listOf(
            mapOf("id" to 1, "title" to "Back to the Future"),
            mapOf("id" to 2, "title" to "A New Hope"),
            mapOf("id" to 3, "title" to "Empire Strikes Back"),
         )
      )
   }

   @Test
   fun `can project a collection selected by its base type`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("loadData", vyne.parseJson("DataResponse", responseJson))
      val results = vyne.query(
         """find { DataResponse } as {
         movies : Film[] as {
            id : FilmId
            title : FilmTitle

            // find all the reviews - FilmReview[] is a base type.
            reviews : FilmReview[]
         }[]
      }
      """
      )
         .typedObjects()
      results.size.should.equal(1)
   }

   @Test
   fun `foo`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Actor {
            actorId : ActorId inherits Int
         }
         model Film {
            filmId : FilmId inherits Int
         }
         service DataService {
            operation findActors():Actor[]
//            operation findFilms(ActorId):Film[]
            operation findFilm(ActorId):Film
         }
      """.trimIndent()
      )
      stub.addResponse("findActors", vyne.parseJson("Actor[]", """[ { "actorId" : 1 } ]"""))
      stub.addResponse("findFilms", vyne.parseJson("Film[]", """[ { "filmId" : 100 } ]"""))
      stub.addResponse("findFilm", vyne.parseJson("Film", """{ "filmId" : 100 } """))

      val result = vyne.query(
         """find { Actor[] } as {
         | actorId : ActorId
         | films : Film
         |}[]
      """.trimMargin()
      )
         .typedObjects()
      result.should.not.be.`null`
   }

   @Test
   fun `when projecting a collection can hoist child attributes higher by selecting them on type`():Unit = runBlocking {
      val (vyne, stub) = testVyne("""
         model Actor {
            name : Name inherits String
         }
         model Movie {
            actors : Actor[]
            title : MovieTitle inherits String
         }
         model CatalogAssets {
            movies : Movie[]
         }
         model Catalog {
            assets : CatalogAssets
         }
         service DataService {
            operation getCatalog():Catalog
         }
      """.trimIndent())
      stub.addResponse("getCatalog", vyne.parseJson("Catalog",
         """{
            | "assets" : {
            |     "movies": [
            |        { "title" : "A new hope", "actors" : [ { "name" : "Jack" } , { "name" : "Mark" } ] },
            |        { "title" : "Back to the Future", "actors" : [ { "name" : "Mike" } , { "name" : "Chris" } ] }
            |     ]
            | }
            |}
         """.trimMargin()))
      val results = vyne.query("""find { Catalog } as {
         | catalogEntries : {
         |   actors : Actor[]
         | }
         |}
      """.trimMargin())
         .rawObjects()
      results.should.not.be.`null`
   }


   @Test
   fun `can project an object selecting an array from a parent using base type`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         schema + """

         service ReviewService {
            operation getReviews(FilmId):FilmReview[]
         }
      """.trimIndent()
      )
      stub.addResponse("loadData", vyne.parseJson("DataResponse", responseJson))
      val results = vyne.query(
         """find { DataResponse } as {
         movies : Film[] as {
            movieId : FilmId
            movieTitle : FilmTitle

            // find all the reviews - FilmReview[] is a base type.
            reviews : FilmReview[]
         }[]
      }
      """
      )
         .typedObjects()
      results.size.should.equal(1)
      val moviesList = results.single()["movies"] as TypedCollection
      moviesList.should.have.size(3)
      moviesList.forEach { movie ->
         val reviews = (movie as TypedObject).get("reviews") as TypedCollection
         // Each movie should have all the reviews.
         // Not sensible, just that's the point of the test.
         reviews.should.have.size(6)
      }
   }


}
