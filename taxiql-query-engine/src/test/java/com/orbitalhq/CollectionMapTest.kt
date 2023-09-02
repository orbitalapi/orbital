package com.orbitalhq

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import com.orbitalhq.models.json.parseJson
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * These tests focus on using the map keyword in a query.
 * map indicates that the input should be iterated and a find performed
 * for each value.
 */
class CollectionMapTest {
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
         listOf(vyne.parseJson("Review", """{ "id" : $filmId, "reviewText" : "Not bad, really" }"""))
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
   fun `can project and map to transform a collection then call a mutation`() : Unit = runBlocking{
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
         listOf(vyne.parseJson("Review", """{ "id" : $filmId, "reviewText" : "Not bad, really" }"""))
      }
      stub.addResponse("saveOne") { _, inputs -> listOf(inputs.single().second) }
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
}
