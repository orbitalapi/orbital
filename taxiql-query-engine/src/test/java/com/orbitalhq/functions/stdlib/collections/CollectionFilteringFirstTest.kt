package com.orbitalhq.functions.stdlib.collections

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.orbitalhq.utils.asA
import org.junit.jupiter.api.Test

class CollectionFilteringFirstTest {
   val schema = TaxiSchema.from(
      """
       model Person {
           id : PersonId inherits Int
           name : PersonName inherits String
          }
          model Movie {
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
   fun `can select the first entry from a collection`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Movie[]", movieJson))
      val results = vyne.query(
         """find { Movie[] } as {
          // Selecting the first person as the star
          starring : Person = first(Person[])
         }[]
      """
      )
         .typedObjects()
      val movie = results.single().asA<TypedObject>()
      movie.toRawObject().shouldBe(
         mapOf(
            "starring" to mapOf(
               "id" to 1,
               "name" to "Jack"
            )
         )
      )
   }
}
