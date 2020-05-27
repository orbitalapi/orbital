package io.vyne.schemas

import io.vyne.models.TypedInstance

typealias TaxiConstraint = lang.taxi.services.operations.constraints.Constraint

interface ConstraintEvaluation {
   val evaluatedValue: TypedInstance
   val violation: ConstraintViolation?

   companion object {
      fun valid(evaluatedValue: TypedInstance) = DefaultConstraintEvaluation(evaluatedValue)
   }

   val isValid
      get() = this.violation == null
}

data class DefaultConstraintEvaluation(override val evaluatedValue: TypedInstance, override val violation: ConstraintViolation? = null) : ConstraintEvaluation

data class ConstraintEvaluations(val evaluatedValue: TypedInstance, val evaluations: List<ConstraintEvaluation>) : List<ConstraintEvaluation> by evaluations {
   val violationCount = evaluations.count { !it.isValid }
   val isValid = violationCount == 0
}

interface Constraint

interface InputConstraint : Constraint {
   // note:
   // This USED to take param: Parameter as the first argument.
   // I've swapped it to type.
   // When evaluating constraints on nested fields (eg., parameter types)
   // using Param becomes awkward.
   // The Param is actually the parent, but the constraint is on the nested attribute
   // Therefore, swapping it out to type.
   // It's possible this may need to be richer to pass additional attributes
   // from the param wrapper, but at present, type is all we're using.
   fun evaluate(argumentType: Type, value: TypedInstance, schema: Schema): ConstraintEvaluation
}

interface OutputConstraint : Constraint
interface InputConstraintProvider : ConstraintProvider<InputConstraint>
interface ContractConstraintProvider : ConstraintProvider<OutputConstraint>

interface ConstraintProvider<out T : Constraint> {
   fun applies(constraint: TaxiConstraint): Boolean
   fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): T
}

interface DeferredConstraintProvider {
   fun buildConstraints(): List<InputConstraint>
}

class EmptyDeferredConstraintProvider : DeferredConstraintProvider {
   override fun buildConstraints(): List<InputConstraint> = emptyList()
}
