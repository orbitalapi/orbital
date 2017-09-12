package io.osmosis.polymer.schemas

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject

interface ConstraintViolationValueUpdater {
   fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance
}

object ReplaceValueUpdater : ConstraintViolationValueUpdater {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance = updatedValue
}

class ReplaceFieldValueUpdater(val originalValue: TypedInstance, val fieldName: String) : ConstraintViolationValueUpdater {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      assert(originalValue is TypedObject, { "Can't replace field within a scalar value: $originalValue" })
      return (originalValue as TypedObject).copy(replacingArgs = mapOf(fieldName to updatedValue))
   }

}

interface ConstraintViolation : ConstraintViolationValueUpdater {
   // TODO : This is cheating a bit.
   // While it _may_ be reasonable for a violation to have an opinion about
   // how it can be resolved (maybe), it shouldn't really know about service
   // contracts.  But, this will work for now.
   fun provideResolutionAdvice(operation: Operation, contract: OperationContract): ResolutionAdvice? = null

   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      val updated =  updater.resolveWithUpdatedValue(updatedValue)
      return updated
   }

   val updater: ConstraintViolationValueUpdater
}

typealias ParamName = String
data class ResolutionAdvice(val operation: Operation, val suggestedParams: Map<ParamName, TypedInstance>) {
   fun containsValueForParam(parameter: Parameter): Boolean {
      // Relaxing this.
      // Originally, parameters must be named.
      // However, if we can find an unambiguous match, use that.
      // Revert to named params only if this becomes problematic
      if (parameter.name != null) {
         return suggestedParams.containsKey(parameter.name)
      }

      // Check to see if the type is unambiguous
      if (operation.parameters.count { it.type == parameter.type } == 1) {
         return suggestedParams.filterValues { it.type == parameter.type }.size == 1
      }

      return false

   }

   fun getParamValue(parameter: Parameter): TypedInstance {
      if (parameter.name != null) {
         return suggestedParams[parameter.name] ?: error("No parameter for name ${parameter.name} found in suggested params.  This shouldn't happen")
      }

      return suggestedParams.values.first { it.type == parameter.type }
   }
}

data class ExpectedConstantValueMismatch(val evaluatedInstance: TypedInstance, val requiredType: Type, val fieldName: String, val expectedValue: TypedInstance, val actualValue: TypedInstance, override val updater: ConstraintViolationValueUpdater) : ConstraintViolation {
   override fun provideResolutionAdvice(operation: Operation, contract: OperationContract): ResolutionAdvice? {
      // TODO : This coupling is dangerous.  But, need to consider an abstraction
      // that allow deducing the same answer without the constraint being aware of contracts
      var resolutionAdvice: ResolutionAdvice? = null

      if (contract.returnType == this.requiredType
         && contract.containsConstraint(ReturnValueDerivedFromParameterConstraint::class.java)
         && contract.containsConstraint(AttributeValueFromParameterConstraint::class.java, { it.fieldName == this.fieldName })
         ) {

         val constraintViolatingParam = contract.constraint(ReturnValueDerivedFromParameterConstraint::class.java).paramName to evaluatedInstance
         val paramToAdjustViolatingField = contract.constraint(AttributeValueFromParameterConstraint::class.java, { it.fieldName == this.fieldName }).parameterName to expectedValue
         resolutionAdvice = ResolutionAdvice(operation, mapOf(constraintViolatingParam, paramToAdjustViolatingField))
      }
      return resolutionAdvice
   }
}
