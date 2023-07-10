package io.vyne.functions

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import io.vyne.typedObjects
import org.junit.Test

class FunctionProjectionTest {
   val schema = TaxiSchema.from(
      """
       closed model Person {
           id : PersonId inherits Int
           name : PersonName inherits String
           credentials : {
            actorsUnionId : ActorsUnionId inherits Int
           }

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
         | { "id" : 1, "name" : "Jack" , "credentials" : { "actorsUnionId" : 1000} },
         | { "id" : 2, "name" : "Sparrow" , "credentials" : { "actorsUnionId" : 2000} },
         | { "id" : 3, "name" : "Butcher", "credentials" : { "actorsUnionId" : 3000}  }
         |]
         |}]
   """.trimMargin()

   @Test
   fun `can project the output of a function`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Movie[]", movieJson))
      val results = vyne.query(
         """find { Movie[] } as {
            // Selecting the first person as the star
             starring : Person = first(Person[]) as {
               starsName : PersonName
            }
         }[]
      """
      )
         .typedObjects()
      val movie = results.single().toRawObject()!!
      movie.shouldBe(mapOf("starring" to mapOf("starsName" to "Jack")))
   }

   @Test
   fun `can hoist a nested attribute of the output of a function`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Movie[]", movieJson))
      val results = vyne.query(
         """find { Movie[] } as {
            // Selecting the first person as the star
             starring : Person = first(Person[]) as {
               starsName : PersonName
               unionId : ActorsUnionId
            }
         }[]
      """
      )
         .typedObjects()
      val movie = results.single().toRawObject()!!
      movie.shouldBe(
         mapOf(
            "starring" to mapOf(
               "starsName" to "Jack",
               "unionId" to 1000
            )
         )
      )
   }

   @Test
   fun `can hoist a nested attribute of the output of a function into a scalar`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Movie[]", movieJson))
      val results = vyne.query(
         """find { Movie[] } as {
            // Selecting the first person as the star
             starring : Person = first(Person[]) as ActorsUnionId
         }[]
      """
      )
         .typedObjects()
      val movie = results.single().toRawObject()!!
      movie.shouldBe(
         mapOf(
            "starring" to 1000
         )
      )
   }
}
