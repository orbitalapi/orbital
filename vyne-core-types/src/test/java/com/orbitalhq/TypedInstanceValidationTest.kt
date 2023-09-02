package com.orbitalhq

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.validation.MandatoryFieldNotNull
import com.orbitalhq.models.validation.ValidationRule
import com.orbitalhq.models.validation.ViolationHandler
import com.orbitalhq.models.validation.failValidationViolationHandler
import com.orbitalhq.models.validation.noOpViolationHandler
import com.orbitalhq.models.validation.validate
import com.orbitalhq.schemas.taxi.TaxiSchema
import org.junit.Test

val doNothingViolationHandler = ViolationHandler { _, _ -> true }

class TypedInstanceValidationTest {
   @Test
   fun typedObjectTest() {
      val schema = TaxiSchema.from(
         """
          model Person {
            firstName: FirstName inherits String
            lastName: LastName inherits String
         }
      """.trimIndent()
      )

      val john =
         TypedInstance.from(
            schema.type("Person"),
            """{ "firstName": "John", "lastName": null }""",
            schema,
            source = Provided
         )

      val validationResult1 = john.validate(
         listOf(
            ValidationRule(
               MandatoryFieldNotNull,
               listOf(doNothingViolationHandler)
            )
         )
      )
      expect(validationResult1).to.be.`true`

      val validationResult2 = john.validate(
         listOf(
            ValidationRule(
               MandatoryFieldNotNull,
               listOf(failValidationViolationHandler())
            )
         )
      )
      expect(validationResult2).to.be.`false`

      val validationResult3 = john.validate(
         listOf(
            ValidationRule(
               MandatoryFieldNotNull,
               listOf(doNothingViolationHandler, failValidationViolationHandler())
            )
         )
      )
      expect(validationResult3).to.be.`false`
   }

   @Test
   fun typedCollectionTest() {
      val schema = TaxiSchema.from(
         """
          model Person {
            firstName: FirstName inherits String
            lastName: LastName inherits String
         }
      """.trimIndent()
      )

      val john =
         TypedInstance.from(
            schema.type("Person[]"),
            """[{ "firstName": "John", "lastName": null }]""",
            schema,
            source = Provided
         )

      var message: String? = null
      val validationResult1 = john.validate(
         listOf(
            ValidationRule(
               MandatoryFieldNotNull,
               listOf(failValidationViolationHandler(), noOpViolationHandler { message = it })
            )
         )
      )
      expect(validationResult1).to.be.`false`
      expect(message).to.be.equal(
         """
         The following items in the collection had empty values:
         Item 0: The fields "lastName" are mandatory but there is no value provided.
      """.trimIndent()
      )
   }

   @Test
   fun noOpValidationHandlerShouldReportThePathWhenAccessorIsDefined() {
      val schema = TaxiSchema.from(
         """
          model Person {
            firstName: FirstName inherits String by column("first_name")
            lastName: LastName inherits String by column("last_name")
         }
      """.trimIndent()
      )

      val john =
         TypedInstance.from(
            schema.type("Person"),
            """{ "firstName": "John", "lastName": null }""",
            schema,
            source = Provided
         )

      var rawMessage: String? = null

      val validationResult1 = john.validate(
         listOf(
            ValidationRule(
               MandatoryFieldNotNull,
               listOf(noOpViolationHandler {
                  rawMessage = it
               })
            )
         )
      )

      rawMessage.should.equal("""The fields "lastName (path = "last_name")}" are mandatory but there is no value provided.""")

   }
}
