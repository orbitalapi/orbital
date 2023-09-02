package com.orbitalhq

import io.kotest.common.runBlocking
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import org.junit.jupiter.api.Test

class MapsTest {

   @Test
   fun `a map type can be parsed`() {
      val (vyne, stub) = testVyne(
         """
         type PenCount inherits Int
         model Person {
            ownedPens : OwnedPens inherits Map<String,PenCount>
         }
      """.trimIndent()
      )
      val parsed = vyne.parseJson(
         "Person", """{
         | "ownedPens" : {
         |   "red" : 1,
         |   "blue" : 4
         |  }
         |}
      """.trimMargin()
      ) as TypedObject
      val parsedMap = parsed["ownedPens"] as TypedObject
      parsedMap.value.shouldHaveSize(2)

      val redCount = parsedMap.value["red"]!!
      redCount.typeName.shouldBe("PenCount")
      redCount.value.shouldBe(1)
   }

   @Test
   fun `a map type can be converted back to a raw object`() {
      val (vyne, stub) = testVyne(
         """
         type PenCount inherits Int
         model Person {
            ownedPens : OwnedPens inherits Map<String,PenCount>
         }
      """.trimIndent()
      )
      val parsed = vyne.parseJson(
         "Person", """{
         | "ownedPens" : {
         |   "red" : 1,
         |   "blue" : 4
         |  }
         |}
      """.trimMargin()
      ) as TypedObject
      val rawObject = parsed.toRawObject()
      rawObject.shouldBe(mapOf("ownedPens" to mapOf("red" to 1, "blue" to 4)))
   }

   @Test
   fun `when projecting a map type it is copied as-is`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type PenCount inherits Int
         model Person {
            name : PersonName inherits String
            ownedPens : OwnedPens inherits Map<String,PenCount>
         }
         service PersonService {
            operation findPerson():Person
         }
      """.trimIndent()
      )

      val parsed = vyne.parseJson(
         "Person", """{
         | "name" : "Jimmy",
         | "ownedPens" : {
         |   "red" : 1,
         |   "blue" : 4
         |  }
         |}
      """.trimMargin()
      ) as TypedObject
      stub.addResponse("findPerson", parsed)

      val projectionResult = vyne.query(
         """find { Person } as {
         |  id :  PersonName
         |  sweetSweetPenCollection : OwnedPens
         |}""".trimMargin()
      )
         .firstRawObject()
      projectionResult.shouldBe(
         mapOf(
            "id" to "Jimmy",
            "sweetSweetPenCollection" to mapOf(
               "red" to 1,
               "blue" to 4
            )
         )
      )
   }
}
