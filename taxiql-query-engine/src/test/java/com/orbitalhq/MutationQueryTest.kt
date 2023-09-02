package com.orbitalhq

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.UnresolvedTypeInQueryException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class MutationQueryTest {

   @Test
   fun `will invoke a write service for a mutation`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            personId: PersonId inherits String
         }

         service Peeps {
            write operation updatePerson(Person) : Person
         }
      """
      )
      stub.addResponse("updatePerson") { _, params ->
         listOf(vyne.parseJson("Person", """{ "personId" : "updated-jimmy" } """))
      }
      val result = vyne.query(
         """given { person:Person = { personId : "jimmy" } }
         call Peeps::updatePerson
      """
      )
         .firstRawObject()
      result.shouldBe(mapOf("personId" to "updated-jimmy"))
      val passedInput = stub.invocations["updatePerson"]!!.get(0)
      passedInput.toRawObject().shouldBe(mapOf("personId" to "jimmy"))
   }

   @Test
   fun `will not invoke a write service when doing a find`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         closed model Person {
            personId: PersonId inherits String
         }

         service Peeps {
            write operation updatePerson(PersonId) : Person
         }
      """
      )
      stub.addResponse("updatePerson") { _, params ->
         listOf(vyne.parseJson("Person", """{ "personId" : "updated-jimmy" } """))
      }
      assertThrows<UnresolvedTypeInQueryException> {
         vyne.query(
            """given { person:PersonId =  "jimmy" }
         find { Person }
      """.trimMargin()
         ).typedObjects()
      }
   }

   @Test
   fun `in a query that finds then mutates the writing operation is only invoked during the mutation`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            personId: PersonId inherits String
            anotherId: AnotherId inherits String
         }

         service Peeps {
            write operation deletePerson(AnotherId) : Person
            operation findPerson(PersonId):Person

         }
      """
      )
      stub.addResponse("findPerson") { _, params ->
         listOf(vyne.parseJson("Person", """{ "personId" : "jimmy", "anotherId" : "found-jimmy" } """))
      }
      stub.addResponse("deletePerson") { _, params ->
         listOf(vyne.parseJson("Person", """{ "personId" : "jimmy", "anotherId" : "deleted-jimmy" } """))
      }
      val result = vyne.query(
         """given { person:PersonId =  "jimmy" }
         find { Person }  // First find,
         call Peeps::deletePerson // then delete
      """.trimMargin()
      )
         .firstRawObject()
      // The result should be from our deletePerson service
      result.shouldBe(mapOf("personId" to "jimmy", "anotherId" to "deleted-jimmy"))

      // When we called findPerson, it should be with the original id
      stub.invocations["findPerson"]!!.single().toRawObject().shouldBe("jimmy")
      // This should be with the value from our discovered service
      stub.invocations["deletePerson"]!!.single().toRawObject().shouldBe("found-jimmy")


   }
}
