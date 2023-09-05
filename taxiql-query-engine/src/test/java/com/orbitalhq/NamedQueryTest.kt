package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Test

class NamedQueryTest {

   @Test
   fun `variables in query that are exposed in given clause are used for discovery`():Unit  = runBlocking {
      val (vyne,stub) = testVyne("""
         type MovieId inherits String
         model Movie {
            id : MovieId
            title : MovieTitle inherits String
         }
         service MovieService {
            operation findMovie(MovieId):Movie
         }
      """.trimIndent())
      stub.addResponse("findMovie", vyne.parseJson("Movie", """
         { "id" : "123" , "title" : "Star Wars" }
      """.trimIndent()))
      val queryResult = vyne.query("""
         query findMovie(movieId : MovieId) {
            given { movieId }
            find { Movie }
         }
      """.trimIndent(), arguments = mapOf("movieId" to "123")
      )
         .firstRawObject()
      queryResult.shouldBe(mapOf("id" to "123", "title" to "Star Wars"))
      stub.invocations["findMovie"]!!.first().value.shouldBe("123")
   }
}
