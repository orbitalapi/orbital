package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Test

class QueryWithGivenTest {

   @Test
   fun `can use params from given in contract`():Unit = runBlocking {
      val (vyne,stub) = testVyne(
         """
         model Film {
            id : FilmId inherits String
            title : FilmTitle inherits String
         }
         service Films {
            operation findFilm(FilmId):Film[]
         }
         """.trimIndent()
      )
      stub.addResponse("findFilm", vyne.parseJson("Film[]", """[{ "id" : "123", "title" : "Star Wars" }]"""))
      val result = vyne.query("""
         given { id : FilmId = "123" }
         find { Film[](FilmId == id) }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf(
         "id" to "123",
         "title" to "Star Wars"
      ))
   }

   @Test
   fun `can use params from given in contract against table query`():Unit = runBlocking {
      val (vyne,stub) = testVyne(
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """

         model Film {
            id : FilmId inherits String
            title : FilmTitle inherits String
         }
         service Films {
            table films:Film[]
         }
         """.trimIndent()
      )
      stub.addResponse("films_findManyFilm", vyne.parseJson("Film[]", """[{ "id" : "123", "title" : "Star Wars" }]"""))
      val result = vyne.query("""
         given { id : FilmId = "123" }
         find { Film[](FilmId == id) }
      """.trimIndent())
         .firstRawObject()
      val operationParams = stub.calls["films_findManyFilm"].first()
      val taxiQLQuery = operationParams.single().toRawObject()!! as String
      taxiQLQuery.shouldBe("""find { lang.taxi.Array<Film>(
FilmId == "123"
) }""")
      result.shouldBe(mapOf(
         "id" to "123",
         "title" to "Star Wars"
      ))
   }
}
