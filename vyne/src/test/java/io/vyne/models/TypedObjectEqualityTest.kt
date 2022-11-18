package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class TypedObjectEqualityTest {
   val schema = TaxiSchema.from(
      """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
            address : {
               streetName : StreetName inherits String
               houseNumber : HouseNumber inherits Int
            }
         }
         model NotAPerson {
            firstName : FirstName
            lastName : LastName
            address : {
               streetName : StreetName
               houseNumber : HouseNumber
            }
         }

      """.trimIndent()
   )


   fun json(lastName: String?, houseNumber: Int?): String? {
      return jacksonObjectMapper()
         .writerWithDefaultPrettyPrinter()
         .writeValueAsString(
            mapOf(
               "firstName" to "Jimmy",
               "lastName" to lastName,
               "address" to mapOf(
                  "streetName" to "Main Street",
                  "houseNumber" to houseNumber
               )
            )
         )
   }

   @Test
   fun `two objects are equal when their values are the same`() {
      val personA = TypedInstance.from(schema.type("Person"), json(lastName = "Jones", houseNumber = 11), schema)
      val personB = TypedInstance.from(schema.type("Person"), json(lastName = "Jones", houseNumber = 11), schema)

      personA.should.equal(personB)
   }

   @Test
   fun `two objects are not the same when nested values differ`() {
      val personA = TypedInstance.from(schema.type("Person"), json(lastName = "Jones", houseNumber = 11), schema)
      val personB = TypedInstance.from(schema.type("Person"), json(lastName = "James", houseNumber = 11), schema)

      personA.should.not.equal(personB)
   }

   @Test
   fun `two objects are the same when they have the same null values`() {
      val personA = TypedInstance.from(schema.type("Person"), json(lastName = "Jones", houseNumber = null), schema)
      val personB = TypedInstance.from(schema.type("Person"), json(lastName = "Jones", houseNumber = null), schema)

      personA.should.equal(personB)
   }

   @Test
   fun `two objects are not the same when they have differing null values`() {
      val personA = TypedInstance.from(schema.type("Person"), json(lastName = "James", houseNumber = 11), schema)
      val personB = TypedInstance.from(schema.type("Person"), json(lastName = "James", houseNumber = null), schema)

      personA.should.not.equal(personB)
   }
   @Test
   fun `two objects are not the same when their properties are equal but their types differ`() {
      val personA = TypedInstance.from(schema.type("NotAPerson"), json(lastName = "Jones", houseNumber = 11), schema)
      val personB = TypedInstance.from(schema.type("Person"), json(lastName = "Jones", houseNumber = 11), schema)

      personA.should.not.equal(personB)
   }

}
