package com.orbitalhq

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import com.orbitalhq.models.json.parseJson
import org.junit.Test
import kotlin.test.assertFailsWith

class QueryWithScopedParamsTest {

   @Test
   fun `can pass a scoped param to Vyne`() = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            filmId : FilmId inherits Int
         }
         service Films {
            operation findFilm(FilmId):Film
         }
      """.trimIndent()
      )
      stub.addResponse("findFilm", vyne.parseJson("Film", """{ "filmId" : 123 }"""))
      val result = vyne.query(
         """query FindFilm( filmId : FilmId, wrongFilmId : FilmId) {
   find { Film( FilmId == filmId ) }
}""", arguments = mapOf("wrongFilmId" to 456, "filmId" to 123)
      ).firstRawObject()

      result.shouldBe(mapOf("filmId" to 123))
      // Make sure the correct arg was passed to the service
      stub.invocations["findFilm"]!!.first().value.shouldBe(123)
   }

   @Test
   fun `fails if argument is not provided`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            filmId : FilmId inherits Int
         }
         service Films {
            operation findFilm(FilmId):Film
         }
      """.trimIndent()
      )
      assertFailsWith<IllegalStateException>("No value was provided for parameter filmId") {
         val result = vyne.query(
            """query FindFilm( filmId : FilmId ) {
   find { Film( FilmId == filmId ) }
}""", arguments = mapOf("wrongFilmId" to 456)
         ).firstRawObject()
      }
   }
}
