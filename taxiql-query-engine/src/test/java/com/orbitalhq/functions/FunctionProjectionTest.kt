package com.orbitalhq.functions

import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedCollection
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.rawObjects
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import com.orbitalhq.typedInstances
import com.orbitalhq.typedObjects
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
             starring : first(Person[]) as {
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
         """
            find { Movie[] } as {
            // Selecting the first person as the star
             starring : first(Person[]) as {
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
             starring : ActorsUnionId = first(Person[]) as ActorsUnionId
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


   @Test
   fun `can call a function in a given block`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model CsvData {
            first: String
            last : String
         }
         function csvToPerson(csvData:CsvData):Person -> { firstName : csvData.first, lastName: csvData.last }
      """.trimIndent()
      )
      val result = vyne.query(
         """
         given { csvData: CsvData = { first: 'Jimmy', last: 'Schmitt' } ,
            person: Person = csvToPerson(csvData)
         } find { Person }
      """.trimIndent()
      )
         .firstRawObject()

      result.shouldBe(
         mapOf(
            "firstName" to "Jimmy", "lastName" to "Schmitt"
         )
      )
   }

   @Test
   fun `can call a function returning a static array`(): Unit = runBlocking {
      // This is where I'm up to.
      // The MemberExpression (in AccessorReader) is Any (or Any[])
      // Looks like a taxi bug
      // Looks like the expression is parsed as Any[],
      // but we should infer the type as Person[]

      val (vyne, stub) = testVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         function getPeople():Person[] -> [
            { firstName : 'Jimmy', lastName: 'Schmitt' },
            { firstName : 'Matt', lastName: 'Plant' }
         ]

      """.trimIndent()
      )
      val result = vyne.query(
         """
         given { Person[] = getPeople() }
         find { Person[] }
      """.trimIndent()
      )
         .rawObjects()

      result.shouldBe(
         listOf(
            mapOf("firstName" to "Jimmy", "lastName" to "Schmitt"),
            mapOf("firstName" to "Matt", "lastName" to "Plant"),
         )
      )
   }

   @Test
   fun `can call a function in a given block returning an array`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model CsvData {
            first1: String
            last1 : String
            first2:  String
            last2 : String
         }
         function csvToPerson(csvData:CsvData):Person[] -> [
            { firstName : csvData.first1, lastName: csvData.last1 },
            { firstName : csvData.first2, lastName: csvData.last2 }
         ]

      """.trimIndent()
      )
      val result = vyne.query(
         """
         given { csvData: CsvData = { first1: 'Jimmy', last1: 'Schmitt', first2: 'Matt', last2: 'Plant' } ,
            person: Person[] = csvToPerson(csvData)
         } find { Person[] }
      """.trimIndent()
      )
         .rawObjects()

      result.shouldBe(
         listOf(
            mapOf("firstName" to "Jimmy", "lastName" to "Schmitt"),
            mapOf("firstName" to "Matt", "lastName" to "Plant")
         )

      )
   }

}
