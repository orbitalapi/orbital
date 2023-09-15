package com.orbitalhq.functions.stdlib.transform

import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import com.orbitalhq.utils.asA
import io.kotest.matchers.shouldBe
import org.junit.Test

class ConvertTest {

   @Test
   fun `can convert from one type to another using convert`() {
      val (vyne, stub) = testVyne(
         """
         model Person {
            name : FirstName inherits String
            age : PersonAge inherits String
         }
         model Dude {
            knownBy : FirstName
         }
         model Thing {
            person : Person
            dude : convert(this.person, Dude)
         }
      """.trimIndent()
      )
      val thing = vyne.parseJson(
         "Thing", """{
         | "person" : { "name" : "Jimmy", "age" : 32 }
         |}
      """.trimMargin()
      ).asA<TypedObject>()
      thing.toRawObject().shouldBe(
         mapOf(
            "person" to mapOf("name" to "Jimmy", "age" to "32"),
            "dude" to mapOf("knownBy" to "Jimmy")
         )
      )
   }
}
