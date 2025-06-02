package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.winterbe.expekt.should
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.orbitalhq.utils.asA
import io.kotest.matchers.shouldBe
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
         | aListers : filter(this.cast, (Person) -> containsString(PersonName, 'a') )
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

   @Test
   fun `applies correct filtering against numeric types`():Unit = runBlocking {
      val (vyne,_) = testVyne("""
         model Friend {
            name: Name inherits String
            age: Age inherits Int
         }
      """.trimIndent())
      val result = vyne.query("""
         given {
            friends: Friend[] = [
               { name: 'Jim', age: 20 },
               { name: 'Jack', age: 80 },
               { name: 'Alice', age: 75 },
               { name: 'Bob', age: 30 }
            ]
         }
         find {
            // Names of seniors (over 70)
            seniorFriends: Friend[] = friends.filter((friend:Friend) -> friend.age > 70) as Name[]
            // Names of young adults (under 30)
            youngFriends: Friend[] = friends.filter((friend:Friend) -> friend.age < 30) as Name[]
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf(
         "seniorFriends" to listOf("Jack", "Alice"),
         "youngFriends" to listOf("Jim")
      ))
   }
}
