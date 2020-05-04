package io.vyne.schemas

import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject

object ReplaceValueUpdater : ConstraintViolationValueUpdater {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance = updatedValue
}

class ReplaceFieldValueUpdater(val originalValue: TypedInstance, val fieldName: String) : ConstraintViolationValueUpdater {
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
         (originalValue as TypedObject).copy(replacingArgs = mapOf(fieldName to updatedValue))
      }
   }

}

/**
 * Advice that indicates the value provided doesn't match a constraint
 * and therefore cannot be used
 */
data class ExpectedConstantValueMismatch(private val evaluatedInstance: TypedInstance, private val requiredType: Type, private val fieldName: String, private val expectedValue: TypedInstance, val actualValue: TypedInstance, override val updater: ConstraintViolationValueUpdater) : ConstraintViolation {
   override fun provideResolutionAdvice(operation: Operation, contract: OperationContract): ResolutionAdvice? {
      // TODO : This coupling is dangerous.  But, need to consider an abstraction
      // that allow deducing the same answer without the constraint being aware of contracts
      var resolutionAdvice: ResolutionAdvice? = null

      if (contract.returnType.fullyQualifiedName == this.requiredType.fullyQualifiedName
         && contract.containsConstraint(ReturnValueDerivedFromParameterConstraint::class.java)
         && contract.containsConstraint(AttributeValueFromParameterConstraint::class.java, { it.fieldName == this.fieldName })
      ) {

         val constraintViolatingParam = contract.constraint(ReturnValueDerivedFromParameterConstraint::class.java).attributePath.path to evaluatedInstance
         val paramToAdjustViolatingField = contract.constraint(AttributeValueFromParameterConstraint::class.java, { it.fieldName == this.fieldName }).attributePath.path to expectedValue
         resolutionAdvice = ResolutionAdvice(operation, mapOf(constraintViolatingParam, paramToAdjustViolatingField))
      }
      return resolutionAdvice
   }
}
