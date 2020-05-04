package io.vyne.schemas

import io.vyne.models.TypedInstance

interface ConstraintViolation : ConstraintViolationValueUpdater {
   // TODO : This is cheating a bit.
   // While it _may_ be reasonable for a violation to have an opinion about
   // how it can be resolved (maybe), it shouldn't really know about service
   // contracts.  But, this will work for now.
   fun provideResolutionAdvice(operation: Operation, contract: OperationContract): ResolutionAdvice? = null

   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      val updated = updater.resolveWithUpdatedValue(updatedValue)
      return updated
   }

   val updater: ConstraintViolationValueUpdater
}

interface ConstraintViolationValueUpdater {
   fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance
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
         // TODO : Using resolvesSameAs here is dangerous, as typealiases can come into play,
         // and we should ensure we're only considering narrowing type aliases.
         return suggestedParams.filterValues { it.type.resolvesSameAs(parameter.type) }.size == 1
      }

      return false

   }

   fun getParamValue(parameter: Parameter): TypedInstance {
      if (parameter.name != null) {
         return suggestedParams[parameter.name]
            ?: error("No parameter for name ${parameter.name} found in suggested params.  This shouldn't happen")
      }

      return suggestedParams.values.first { it.type.resolvesSameAs(parameter.type) }
   }
}
