package com.orbitalhq

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ObjectCreationTest {
   @Test
   fun `can declare an object`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
      """.trimIndent()
      )
      val result = vyne.query(
         """
         given { Person = { firstName : 'Jim' , lastName: 'Schmitt' } }
         find { Person }
      """.trimIndent()
      )
         .firstRawObject()
         .shouldBe(
            mapOf(
               "firstName" to "Jim",
               "lastName" to "Schmitt"
            )
         )
   }

   @Test
   fun `can declare an array`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
      """.trimIndent()
      )
      val result = vyne.query(
         """
         given { Person[] = [
            { firstName : 'Jim' , lastName: 'Schmitt' },
            { firstName : 'Jack' , lastName: 'Jones' }
         ] }
         find { Person[] }
      """.trimIndent()
      )
         .typedInstances()

      result.map { it.toRawObject() }
         .shouldBe(
            listOf(
               mapOf("firstName" to "Jim", "lastName" to "Schmitt"),
               mapOf("firstName" to "Jack", "lastName" to "Jones"),
            )
         )
   }
}
