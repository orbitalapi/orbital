package com.orbitalhq.functions.stdlib.collections

import com.winterbe.expekt.should
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.orbitalhq.utils.asA
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CollectionFilteringTest {
   val schema = TaxiSchema.from(
      """
       closed model Person {
           id : PersonId inherits Int
           name : PersonName inherits String
          }
          closed model Movie {
            cast : Person[]
         }
          service PersonService {
            operation getAll():Movie[]
         }
   """.trimIndent()
   )
   val movieJson = """[{
         | "cast" : [
         | { "id" : 1, "name" : "Jack" },
         | { "id" : 2, "name" : "Sparrow" },
         | { "id" : 3, "name" : "Butcher" }
         |]
         |}]
   """.trimMargin()

   @Test
   fun `can filter a list of types from a property on the projected type`():Unit = runBlocking{
      val (vyne,stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Movie[]", movieJson))
      val results = vyne.query("""find { Movie[] } as {
         | cast : Person[]
         | // Filtering directly on a field on this type.
         | aListers : filterAll(this.cast, (Person) -> containsString(PersonName, 'a') )
         |}[]
      """.trimMargin())
         .typedObjects()
      val movie = results.single().asA<TypedObject>()
      val starring = movie.get("aListers").toRawObject()
      starring.should.equal(listOf(
         mapOf("id" to 1, "name" to "Jack"),
         mapOf("id" to 2, "name" to "Sparrow"),
      ))
   }
}
