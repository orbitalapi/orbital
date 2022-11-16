package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import kotlinx.coroutines.runBlocking
import org.junit.Test

class VyneProjectionBuilderTest {

   @Test
   fun `does not attempt to build closed objects`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         model Film {
            title : FilmTitle inherits String
            id : FilmId inherits Int
         }
         closed model Review {
            reviewText: ReviewText inherits String
         }
         service DataService {
            operation loadFilms():Film[]
            operation loadReviews(FilmId):Review
         }
      """.trimIndent())

      stub.addResponse("loadFilms", vyne.parseJson("Film[]", """[
         | { "id" : 1, "title" : "Star Wars" }
         |]""".trimMargin()))
      // Intentionally not wiring the stub, so discovery fails
//      stub.addResponse("loadReviews", vyne.parseJson("Review", """{ "reviewText" : "Not bad" }"""))
      val result = vyne.query("""find { Film[] } as {
         |  title : FilmTitle
         |  review : Review
         |}[]
      """.trimMargin())
         .typedObjects()
      result.should.not.be.`null`

   }
}
