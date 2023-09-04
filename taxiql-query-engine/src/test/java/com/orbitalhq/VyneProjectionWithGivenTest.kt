package com.orbitalhq

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import com.orbitalhq.models.json.parseJson
import org.junit.jupiter.api.Test

class VyneProjectionWithGivenTest {

   @Test
   fun `can pass rich object in given clause and query it to build something`() = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Movie {
            id : MovieId inherits String
            title : MovieTitle inherits String
         }
         model MovieReview {
            score : ReviewScore inherits Int
         }
         service Reviews {
            operation getReview(id:MovieId): MovieReview
         }
      """.trimIndent()
      )
      stub.addResponse("getReview", vyne.parseJson("MovieReview", """{ "score" : 3 }"""))
      val queryResult = vyne.query(
         """
         given { Movie = { id : "1" , title : "Star Wars" } }
         find { MovieReview }
      """.trimIndent()
      )
         .firstRawObject()
      queryResult.shouldBe(mapOf("score" to 3))
   }

   @Test
   fun `can pass rich object in given clause and query it to build an anonymous object in find clause`() = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Movie {
            id : MovieId inherits String
            title : MovieTitle inherits String
         }
         model MovieReview {
            score : ReviewScore inherits Int
         }
         service Reviews {
            operation getReview(id:MovieId): MovieReview
         }
      """.trimIndent()
      )
      stub.addResponse("getReview", vyne.parseJson("MovieReview", """{ "score" : 3 }"""))
      val queryResult = vyne.query(
         """
         given { Movie = { id : "1" , title : "Star Wars" } }
         find {
            title : MovieTitle
            reviewScore : ReviewScore
          }
      """.trimIndent()
      )
         .firstRawObject()
      queryResult.shouldBe(
         mapOf(
            "title" to "Star Wars",
            "reviewScore" to 3
         )
      )
   }
}
