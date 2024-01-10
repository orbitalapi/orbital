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


   @Test
   fun `can evaluate expression in given clause`():Unit = runBlocking {
      val (vyne) = testVyne("")
       vyne.query("""given { message : String = upperCase("hello") }
          |find { String }
       """.trimMargin())
          .firstRawValue()
          .shouldBe("HELLO")
   }

   @Test
   fun `can evaluate expression that references constant in given clause`():Unit = runBlocking {
      val (vyne) = testVyne("type Result inherits String")
      vyne.query("""given { message : String = "hello", result : Result = upperCase(message) }
          |find { Result }
       """.trimMargin())
         .firstRawValue()
         .shouldBe("HELLO")
   }

   @Test
   fun `can evaluate expression that references another expression in given clause`():Unit = runBlocking {
      val (vyne) = testVyne("type Result inherits String")
      vyne.query("""given { message : String = "Hello", upper : String = upperCase(message), result : Result = lowerCase(message) }
          |find { Result }
       """.trimMargin())
         .firstRawValue()
         .shouldBe("hello")
   }
}
