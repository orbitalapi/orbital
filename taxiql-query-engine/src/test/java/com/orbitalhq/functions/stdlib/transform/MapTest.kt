package com.orbitalhq.functions.stdlib.transform

import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedInstace
import com.orbitalhq.rawObjects
import com.orbitalhq.testVyne
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.handleCoroutineException
import kotlinx.coroutines.runBlocking
import lang.taxi.CompilationException
import lang.taxi.shouldContainMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MapTest {
   @Test
   fun `can use map to convert items in an array`(): Unit = runBlocking {
      val (vyne) = testVyne(
         """
         model Film {
            title : Title inherits String
            year : Year inherits Int
         }
      """.trimIndent()
      )
      val result = vyne.query(
         """
         given { Film[] = [{ title: "Star Wars", year: 1979 }, { title: "Jaws", year: 1984 }] }
         find { Film[].map ( (Title) -> Title.upperCase() ) }
      """.trimIndent()
      )
         .rawResults.toList()
      result.shouldNotBeNull()
      result.shouldBe(listOf("STAR WARS", "JAWS"))
   }

   @Test
   fun `can project from one array to another using a map function`(): Unit = io.kotest.common.runBlocking {
      val (vyne, stub) = testVyne(
         """
         closed model PersonResponse {
           requestId : RequestId inherits String
           people: Person[]
         }
         closed model Person {
           name : Name inherits String
         }

         service PersonService {
           operation getPeople():PersonResponse
         }

         model Person2 {
           NAME : Name
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getPeople", """{
   "requestId" : "123",
   "people" : [ { "name" : "Jimmy" } , { "name" : "Jack" } ]
}"""
      )

      val result = vyne.query(
         """find { PersonResponse } as {
    REQUEST_ID : RequestId
    PEOPLE : Person[].map((Person) -> Person2)
}"""
      ).firstRawObject()
      result.shouldNotBeNull()
   }


   // ORB-995
   // Adding this test here, as this is how it was discovered,
   // but the root issue was duplicating scoped facts in a cascading fact bag
   @Test
   fun `using an array selector to select single item to map`(): Unit = runBlocking {
      val (vyne, _) = testVyne(
         """
         model Person {
            name : Name inherits String
         }

         model Dude {
            handle: Name
         }
      """.trimIndent()
      )

      val result = vyne.query("""
         given {
            p : Person = {
               name : "Jimmy"
            }
         }
         find { peeps: Dude[] =  Person[].map((Person) -> Dude)
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("peeps" to listOf(mapOf("handle" to "Jimmy"))))
   }
}
