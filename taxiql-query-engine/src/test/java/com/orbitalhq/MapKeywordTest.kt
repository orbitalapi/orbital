package com.orbitalhq

import com.orbitalhq.models.json.right
import com.orbitalhq.models.json.tryParseJson
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * These tests focus on using the map keyword in a query.
 * map indicates that the input should be iterated and a find performed
 * for each value.
 */
class MapKeywordTest {
   @Test
   fun `can use map keyword to project a collection type`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
            title : FilmTitle inherits String
         }
         model Review {
            id : FilmId
            reviewText : ReviewText inherits String
         }
         service ReviewService {
            operation getReview(FilmId):Review
         }
      """.trimIndent()
      )
      stub.addResponse("getReview") { _, inputs ->
         val filmId = inputs[0].second.value!!
         listOf(vyne.tryParseJson("Review", """{ "id" : $filmId, "reviewText" : "Not bad, really" }"""))
      }
      val queryResult = vyne.query(
         """
         given { input: Film[] = [ { id : 1, title: "Back to the Future" }, { id : 2, title: "Star Wars" } ] }
         map { Review } as {
            filmId : FilmId
            name : FilmTitle
            review: ReviewText
         }
      """
      ).rawObjects()
      queryResult.shouldContainExactlyInAnyOrder(
         listOf(
            mapOf(
               "filmId" to 1,
               "name" to "Back to the Future",
               "review" to "Not bad, really"
            ),
            mapOf(
               "filmId" to 2,
               "name" to "Star Wars",
               "review" to "Not bad, really"
            )
         )
      )
   }

   @Test
   fun `can project and map to transform a collection then call a mutation`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
            title : FilmTitle inherits String
         }
         model Review {
            id : FilmId
            reviewText : ReviewText inherits String
         }
         // field naming to make assertions clearer
         parameter model FilmAndReview {
            filmReviewId : FilmId
            filmReviewTitle : FilmTitle
            filmReviewText: ReviewText
         }
         service ReviewService {
            operation getReview(FilmId):Review
            write operation saveOne( FilmAndReview ) : FilmAndReview
         }
      """.trimIndent()
      )
      stub.addResponse("getReview") { _, inputs ->
         val filmId = inputs[0].second.value!!
         listOf(vyne.tryParseJson("Review", """{ "id" : $filmId, "reviewText" : "Not bad, really" }"""))
      }
      stub.addResponse("saveOne") { _, inputs -> listOf(inputs.single().second.right()) }
      val queryResult = vyne.query(
         """
         given { input: Film[] = [ { id : 1, title: "Back to the Future" }, { id : 2, title: "Star Wars" } ] }
         map { Review } as {
            filmId : FilmId
            name : FilmTitle
            review: ReviewText
         }
         call ReviewService::saveOne
      """
      ).rawObjects()
      queryResult.shouldContainExactlyInAnyOrder(
         listOf(
            mapOf(
               "filmReviewId" to 1,
               "filmReviewTitle" to "Back to the Future",
               "filmReviewText" to "Not bad, really"
            ),
            mapOf(
               "filmReviewId" to 2,
               "filmReviewTitle" to "Star Wars",
               "filmReviewText" to "Not bad, really"
            )
         )
      )
   }

   @Test
   fun `can use map keyword to iterate and call a mutation with multiple facts in scope`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name :  PersonName inherits String
            points : Points inherits Int
            score : Score inherits Int
         }
         parameter model PersonUpdate {
            called : PersonName
            newPoints : NewPoints
         }
         // This is to ensure that projection is happening correctly, and that
         // evals are performed with the correct scope
         type NewPoints inherits Int = (Points,Score) -> Points + Score
         service PersonApi {
            write operation saveOne( PersonName, ApiKey, PersonUpdate ) : PersonUpdate
         }
      """.trimIndent()
      )
      stub.addResponseReturningInputs("saveOne")
      val result = vyne.query(
         """
         given {
            ApiKey = "IAmTheApiKey",
            Person[] = [
               { name : "Jimmy", points : 2, score: 3 },
               { name : "Jack", points : 4, score: 5 }
            ]
         }
         map { Person[] }
         call PersonApi::saveOne
      """.trimIndent()
      )
         .typedInstances()

      val calls = stub.calls["saveOne"]
      calls.shouldHaveSize(2)

      // The order of the calls isn't guaranteed because we run in parallel,
      // so find each of the calls and assert
      val jimmyCall = calls.single { params -> params[0].toRawObject().toString() == "Jimmy" }
      jimmyCall.map { it.toRawObject() }.shouldBe(
         listOf(
            "Jimmy",
            "IAmTheApiKey",
            mapOf("called" to "Jimmy", "newPoints" to 5)
         )
      )
      val jackCall = calls.single { params -> params[0].toRawObject().toString() == "Jack" }
      jackCall.map { it.toRawObject() }.shouldBe(
         listOf(
            "Jack",
            "IAmTheApiKey",
            mapOf("called" to "Jack", "newPoints" to 9)
         )
      )

   }
}
