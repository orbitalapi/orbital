package io.vyne

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.validation.MandatoryFieldNotNull
import io.vyne.models.validation.ValidationRule
import io.vyne.models.validation.ViolationHandler
import io.vyne.models.validation.failValidationViolationHandler
import io.vyne.models.validation.noOpViolationHandler
import io.vyne.models.validation.validate
import io.vyne.schemas.taxi.TaxiSchema
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
