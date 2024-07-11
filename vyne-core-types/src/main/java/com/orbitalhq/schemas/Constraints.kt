package com.orbitalhq.schemas

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject

interface ConstraintEvaluation {
   val evaluatedValue: TypedInstance
   val violation: ConstraintViolation?

   companion object {
      fun valid(evaluatedValue: TypedInstance) = DefaultConstraintEvaluation(evaluatedValue)
   }

   val isValid
      get() = this.violation == null
}

data class DefaultConstraintEvaluation(override val evaluatedValue: TypedInstance, override val violation: ConstraintViolation? = null) : ConstraintEvaluation {
   override fun toString(): String {
      return "DefaultConstraintEvaluation - evaluatedValue: ${evaluatedValue.typeName} ${evaluatedValue.toRawObject()} , violation: $violation"
   }
}

data class ConstraintEvaluations(val evaluatedValue: TypedInstance, val evaluations: List<ConstraintEvaluation>) : List<ConstraintEvaluation> by evaluations {
   val violationCount = evaluations.count { !it.isValid }
   val isValid = violationCount == 0
}


data class NestedConstraintEvaluation(val parent: TypedObject, val fieldName: String, private val evaluation: ConstraintEvaluation) : ConstraintEvaluation {
   override val evaluatedValue: TypedInstance = evaluation.evaluatedValue
   override val violation: ConstraintViolation?
      get() {
         if (evaluation.violation == null) return null
         return NestedConstraintViolation(evaluation.violation!!, this)
      }
}

class NestedConstraintViolation(violation: ConstraintViolation, private val evaluation: NestedConstraintEvaluation) : ConstraintViolation by violation {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      val resolvedParent = evaluation.parent.copy(mapOf(evaluation.fieldName to updatedValue))
      return resolvedParent
   }
}
