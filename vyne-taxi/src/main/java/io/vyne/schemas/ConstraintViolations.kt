package io.vyne.schemas

import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import lang.taxi.services.operations.constraints.*

object ReplaceValueUpdater : ConstraintViolationValueUpdater {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance = updatedValue
}

class ReplaceFieldValueUpdater(private val originalValue: TypedInstance, val identifier: PropertyIdentifier) : ConstraintViolationValueUpdater {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      assert(originalValue is TypedObject, { "Can't replace field within a scalar value: $originalValue" })

      // This needs a test.
      // The idea here is that sometimes we get responses that contain replacement attributes,
      // (eg., pluck foo from the response, and copy it to foo of the original.
      //
      // Sometimes the replacements are wholesale replacements, and copying just the requested
      // field isn't enough.
      // For now, I'm assuming if the originalValue and updatedValue are the same type, then it's
      // a wholesale replacement.
      // However, that feels coincidental, there's a whole bunch of scenarios where you asked valueA
      // to be updated, and in doing so, valueB is also different.  (eg., currency conversion).
      return if (originalValue.type.name == updatedValue.type.name) {
         updatedValue
      } else {
         TODO()
//         (originalValue as TypedObject).copy(replacingArgs = mapOf(fieldName to updatedValue))
      }
   }

}

/**
 * Advice that indicates the value provided doesn't match a constraint
 * and therefore cannot be used
 */
data class ExpectedConstantValueMismatch(private val evaluatedInstance: TypedInstance, private val requiredType: Type, private val property: PropertyIdentifier, private val expectedValue: TypedInstance, val actualValue: TypedInstance, override val updater: ConstraintViolationValueUpdater) : ConstraintViolation {
   override fun provideResolutionAdvice(operation: Operation, contract: OperationContract): ResolutionAdvice? {
      // TODO : This coupling is dangerous.  But, need to consider an abstraction
      // that allow deducing the same answer without the constraint being aware of contracts
      var resolutionAdvice: ResolutionAdvice? = null

      if (contract.returnType.fullyQualifiedName == this.requiredType.fullyQualifiedName
         && contract.containsConstraint(ReturnValueDerivedFromParameterConstraint::class.java)
         && contract.containsConstraint(PropertyToParameterConstraint::class.java) { it.propertyIdentifier == this.property }
      ) {

         val constraintViolatingParam = contract.constraint(ReturnValueDerivedFromParameterConstraint::class.java).propertyIdentifier to evaluatedInstance
         val paramToAdjustViolatingField = contract.constraint(PropertyToParameterConstraint::class.java) { it.propertyIdentifier == this.property }.expectedValue.asParameterIdentifier() to expectedValue

         resolutionAdvice = ResolutionAdvice(operation, mapOf(constraintViolatingParam, paramToAdjustViolatingField))
      }
      return resolutionAdvice
   }
}

// This is a hack while I'm working on failing tests
// From what I can see, we need some way to indicate the name of the parameter that needs to be updated.
private fun ValueExpression.asParameterIdentifier(): PropertyIdentifier {
   return when (this) {
      is ConstantValueExpression -> error("I don't know what to do in this situation yet, let's see what the scneario looks like")
      is RelativeValueExpression -> PropertyFieldNameIdentifier(this.path)
   }
}
