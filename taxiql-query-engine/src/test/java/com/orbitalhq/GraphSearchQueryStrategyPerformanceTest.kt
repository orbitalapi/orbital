package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.Benchmark
import com.orbitalhq.utils.asA
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import lang.taxi.types.ObjectType
import org.junit.Test

class GraphSearchQueryStrategyPerformanceTest {

   @Test
   fun `build and search a large graph`():Unit = runBlocking{
      val (vyne,stub) = testVyne("""
         model Film {
            id : FilmId inherits Int
            title : FilmTitle inherits String
          }
          model FilmReview {
            score : ReviewScore inherits Int
          }
          model IdResolution {
            filmId : FilmId
            squashedTomatoesId : SquashedTomatoesId inherits Int
         }
         model StreamingServiceProvider {
            name : ServiceProviderName inherits String
            price : Price inherits Decimal
         }
         service FilmsServices {
            operation getFilms():Film[]
            operation getReview(SquashedTomatoesId):FilmReview
            operation getIds(FilmId):IdResolution
            operation getStreamingProvider(FilmId):StreamingServiceProvider
         }
      """)
      stub.addResponse("getFilms", vyne.parseJson("Film[]", """[
         |{ "id" : 1 , "title" : "Star Wars" },
         |{ "id" : 2 , "title" : "Empire Strikes Back" },
         |{ "id" : 3 , "title" : "Return of the Jedi" }
         |]
      """.trimMargin()))
      val filmReview = vyne.parseJson("FilmReview", """{ "score"  : 4 }""")
      stub.addResponse("getReview", filmReview)
      stub.addResponse("getIds" , vyne.parseJson("IdResolution", """{ "filmId" : 1, "squashedTomatoesId" : 100 }"""))
      stub.addResponse("getStreamingProvider" , vyne.parseJson("StreamingServiceProvider", """{ "name" : "Netflix", "price" : 2.99 }"""))

      val result =       vyne.query("""find { Film[] } as {
         |  id : FilmId
         |  title : FilmTitle
         |  score : ReviewScore
         |  price : Price
         |}[]
      """.trimMargin())
         .rawObjects()
      result.shouldHaveSize(3)
      result.shouldBe(listOf(
         mapOf("id" to 1, "title" to "Star Wars", "score" to 4, "price" to 2.99.toBigDecimal()),
         mapOf("id" to 2, "title" to "Empire Strikes Back", "score" to 4, "price" to 2.99.toBigDecimal()),
         mapOf("id" to 3, "title" to "Return of the Jedi", "score" to 4, "price" to 2.99.toBigDecimal()),
      ))


      Benchmark.benchmark("graph query", iterations = 500) {
         runBlocking {
            vyne.query("""find { Film[] } as {
         |  id : FilmId
         |  title : FilmTitle
         |  score : ReviewScore
         |  price : Price
         |}[]
      """.trimMargin())
               .rawObjects()
         }

      }



   }
}
