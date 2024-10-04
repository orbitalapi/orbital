package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class CollectionFilteringIntersectionTest {
   val schema = TaxiSchema.from(
      """
         model Person {
           name : PersonName inherits String
          }
          model Response {
            groupA:Person[]
            groupB:Person[]
          }
          service PersonService {
            operation getAll():Response
         }
   """.trimIndent()
   )
   @Test
   fun `returns the shared elements`():Unit = runBlocking {
      val (vyne,stub) = testVyne(schema)
      stub.addResponse("getAll", """
         {"groupA" : [{"name" : "Jimmy"}, {"name" : "Bob"}, {"name" : "Jane" }],
         "groupB" :  [{"name" : "Jack"}, {"name" : "Bob"}, {"name" : "Jane" }]
         }""".trimIndent())

      val response = vyne.query("""find { Response } as (response:Response) -> {
         |  union : PersonName[] by intersection(response.groupA, response.groupB).convert(PersonName[])
         |}
      """.trimMargin())
         .firstRawObject()
      response.shouldBe(mapOf("union" to listOf("Bob", "Jane")))
   }

   @Test
   fun `returns an empty list if none provided`():Unit = runBlocking {
      val (vyne,stub) = testVyne(schema)
      stub.addResponse("getAll", """
         {"groupA" : [{"name" : "Jimmy"}, {"name" : "Bob"}, {"name" : "Jane" }],
         "groupB" :  [{"name" : "Jack"}, {"name" : "Bobby"}, {"name" : "Janey" }]
         }""".trimIndent())

      val response = vyne.query("""find { Response } as (response:Response) -> {
         |  union : PersonName[] by intersection(response.groupA, response.groupB).convert(PersonName[])
         |}
      """.trimMargin())
         .firstRawObject()
      response.shouldBe(mapOf("union" to emptyList<String>()))
   }
   @Test
   fun `returns an empty list if one of the inputs is empty`():Unit = runBlocking {
      val (vyne,stub) = testVyne(schema)
      stub.addResponse("getAll", """
         {"groupA" : [],
         "groupB" :  [{"name" : "Jack"}, {"name" : "Bobby"}, {"name" : "Janey" }]
         }""".trimIndent())

      val response = vyne.query("""find { Response } as (response:Response) -> {
         |  union : PersonName[] by intersection(response.groupA, response.groupB).convert(PersonName[])
         |}
      """.trimMargin())
         .firstRawObject()
      response.shouldBe(mapOf("union" to emptyList<String>()))
   }

}
