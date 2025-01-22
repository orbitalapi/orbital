package com.orbitalhq

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test


class ScopeTraversalTest {

   @Test
   fun `when a scope defines a value as null then we do not search elsewhere to find a projectable value`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         model Award {
            name : AwardName inherits String
         }
         model Person {
            name : PersonName inherits String
            award : Award?
         }
         model Movie {
            cast : Person[]
            award : Award?
         }
         service MovieApi {
            operation findMovie():Movie
         }
      """.trimIndent())
      stub.addResponse("findMovie", """
         {
            "cast" : [ { "name" : "Johnny", "award" : null  } ],
            "award" : { "name" : "Best Film" }
         }
      """.trimIndent())
      val queryResult = vyne.query("""find { Movie } as {
         |  cast : Person[] as {
         |     actorName : PersonName
         |     // This should be null, as it's populated with null
         |     // on the Person object, so we should not traverse upwards to popular it
         |     castAward : AwardName
         |  }[]
         |  movieAward : AwardName
         |}
      """.trimMargin())
         .firstRawObject()
      queryResult.shouldBe(
         mapOf("movieAward" to "Best Film",
            "cast" to listOf(
               mapOf("actorName" to "Johnny", "castAward" to null)
            ))
      )
   }
}
