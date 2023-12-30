package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

class OperationContractTest {

   @Test
   fun `will invoke the correct service for a date range query`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type ReleaseDate inherits Date
         model Movie {
            title : String
         }
         service Movies {
            operation getMoviesReleasedBetween(startDate:ReleaseDate,endDate:ReleaseDate):Movie[](ReleaseDate > startDate && ReleaseDate < endDate)
         }
      """.trimIndent()
      )
      stub.addResponse("getMoviesReleasedBetween", vyne.parseJson("Movie[]", """[{ "title" : "STar Wars" }]"""))
      val results = vyne.query("""find { Movie[]( ReleaseDate > '2023-11-01' && ReleaseDate < '2023-10-01') }""")
         .rawObjects()
      results.shouldHaveSize(1)
   }
}
