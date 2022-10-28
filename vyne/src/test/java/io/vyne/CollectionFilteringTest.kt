package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CollectionFilteringTest {

   val schema = TaxiSchema.from(
      """
      model Person {
         firstName : FirstName inherits String
         lastName: LastName inherits String
      }

      service TestService {
         operation getAll():Person[]
      }
   """.trimIndent()
   )
   val peopleJson = """[
      |{ "firstName" : "Jimmy" , "lastName" : "Spitts" },
      |{ "firstName" : "Johnny", "lastName" : "Splott" },
      |{ "firstName" : "Jack", "lastName"  :"Splatt" }
      |]
   """.trimMargin()

   @Test
   fun `can filter a subset of a collection based on a predicate`():Unit = runBlocking {
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Person[]", peopleJson))
      val result = vyne.query("find { Person[] } as Person( true == true )[]")
         .rawObjects()
      result.should.have.size(3)
      TODO()

   }

}
