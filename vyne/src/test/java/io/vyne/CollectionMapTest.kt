package io.vyne

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.vyne.models.json.parseJson
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
}
