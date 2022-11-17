package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.TypedNull
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.runBlocking
import org.junit.Test

class VyneProjectionBuilderTest {

   val schema = TaxiSchema.from("""
        model Film {
            title : FilmTitle inherits String
            id : FilmId inherits Int
         }
         closed model Review {
            rating : Rating
            reviewText: ReviewText inherits String
         }
         closed model Rating {
            reviewScore : ReviewScore inherits Int
         }
         service DataService {
            operation loadFilms():Film[]
            operation loadReviews(FilmId):Review
         }
   """.trimIndent())

//   @Test
   fun `does not attempt to build closed objects`():Unit = runBlocking {
      val (vyne,stub) = testVyne(schema)

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
         .firstTypedObject()

      result["review"].should.be.instanceof(TypedNull::class.java)
   }

   @Test
   fun `does populate field that is selected from within closed object if returned from service`():Unit = runBlocking {
      // If a service returns an object, we should be
      // able to pick objects from within it.
      val (vyne,stub) = testVyne(schema)

      stub.addResponse("loadFilms", vyne.parseJson("Film[]", """[
         | { "id" : 1, "title" : "Star Wars" }
         |]""".trimMargin()))
      stub.addResponse("loadReviews", vyne.parseJson("Review", """{ "reviewText" : "Not bad", "rating" : { "reviewScore"  :4 } }"""))
      val result = vyne.query("""find { Film[] } as {
         |  title : FilmTitle
         |  rating : Rating
         |}[]
      """.trimMargin())
         .firstTypedObject()

      result["review"].should.be.instanceof(TypedNull::class.java)
   }
}
