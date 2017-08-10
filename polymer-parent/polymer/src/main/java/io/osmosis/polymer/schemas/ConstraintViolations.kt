package io.osmosis.polymer.schemas

import io.osmosis.polymer.models.TypedInstance

interface ConstraintViolation {
   // TODO : This is cheating a bit.
   // While it _may_ be reasonable for a violation to have an opinion about
   // how it can be resolved (maybe), it shouldn't really know about service
   // contracts.  But, this will work for now.
   fun provideResolutionAdvice(operation: Operation, contract: OperationContract): ResolutionAdvice? = null
}

typealias ParamName = String
data class ResolutionAdvice(val operation: Operation, val suggestedParams: Map<ParamName, TypedInstance>) {
   fun containsValueForParam(parameter: Parameter): Boolean {
      if (parameter.name == null) {
         return false
      }
      return suggestedParams.containsKey(parameter.name)
   }

   fun getParamValue(parameter: Parameter): TypedInstance {
      assert(parameter.name != null, { "Parameter must be named" })
      return suggestedParams[parameter.name!!] ?: error("No parameter for name ${parameter.name} found in suggested params.  This shouldn't happen")
   }
}

data class ExpectedConstantValueMismatch(val evaluatedInstance: TypedInstance, val requiredType: Type, val fieldName: String, val expectedValue: TypedInstance, val actualValue: TypedInstance) : ConstraintViolation {
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
