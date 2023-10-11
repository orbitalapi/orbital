package com.orbitalhq

import com.winterbe.expekt.should
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.json.parseJsonModel
import com.orbitalhq.schemas.taxi.TaxiSchema
import org.junit.Test


class VyneEncodingTest {

   @Test
   fun `Type and data using Polish accented characters`() {
      val testSchema = """
         enum Roślina {
            Żeńszeń("Pomaga dosłownie na wszystko"),
            Mniszek("Pomoaga w chorobach skóry")
         }

         type Recepta {
            typRośliny: Roślina
         }

      """.trimIndent()

      val (vyne, stubService) = testVyne(testSchema)
      val result = TypedInstance.from(vyne.type("Recepta"), """
         {
            "typRośliny": "Żeńszeń"
         }
         """.trimIndent(), vyne.schema)
         .toRawObject() as Map<String,Any>
      result["typRośliny"].should.equal("Żeńszeń")
   }

   fun `Unicode not supported in in enum values`() {
      val (errors, doc) = TaxiSchema.compiled("""
         enum Plant {
            Zensen("Helps for everything from \u0041 to \u0042"),
            Dendelion("Helps with skin")
         }

         type Prescription {
            plantType: Plant
         }

      """)
      errors.should.not.be.empty
   }

   @Test
   fun `Unicode support in values`() {
      val testSchema = """
         type Prescription {
            description: String
         }
      """.trimIndent()

      val (vyne, stubService) = testVyne(testSchema)
      val result = vyne.parseJsonModel("Prescription", """
         {
            "description": "\u0041 - Stay at home, \u0042 - Drink lemon and ginger tea"
         }
         """.trimIndent())

      (result.value as Map<String, TypedValue>)["description"]?.value.should.equal("A - Stay at home, B - Drink lemon and ginger tea")
   }

}

