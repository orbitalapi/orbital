package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QueryWithServiceRestrictionsTest {

   @Test
   fun `when services have inclusions specified other services are not called`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         model Film {
            title : Title inherits String
         }
         service FilmService {
            operation getFilms():Film[]
            operation getBlockbusters():Film[]
         }
      """.trimIndent())
      stub.addResponse("getFilms", vyne.parseJson("Film[]", "[]"))
      stub.addResponse("getBlockbusters", vyne.parseJson("Film[]", "[]"))

      vyne.query("""
         find { Film[] }
         using { FilmService::getFilms }
      """.trimIndent())
         .rawObjects()


      stub.callCount("getFilms").shouldBe(1)
      stub.callCount("getBlockbusters").shouldBe(0)
   }



}
