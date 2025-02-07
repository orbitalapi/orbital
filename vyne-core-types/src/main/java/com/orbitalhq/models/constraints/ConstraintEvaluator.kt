package com.orbitalhq.models.constraints

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.ConstraintEvaluation
import lang.taxi.services.operations.constraints.Constraint
import mu.KotlinLogging

/**
 * A convenience that makes calling ExpressionConstraint.evaluate()
 * easier, holding the "context stuff"
 */
interface ConstraintEvaluator {
   fun evaluate(constraint: Constraint, value: TypedInstance): ConstraintEvaluation
   fun allConstraintsAreValid(constraints: List<Constraint>, value: TypedInstance): Boolean {
      return constraints.all { constraint ->
         evaluate(constraint, value).isValid
      }
   }
}

object NoopConstraintEvaluator : ConstraintEvaluator {
   private val logger = KotlinLogging.logger {}
   override fun evaluate(constraint: Constraint, value: TypedInstance): ConstraintEvaluation {
      logger.warn { "Constraint ${constraint.asTaxi()} was not evaluated against an instance of ${value.type.name.shortDisplayName} because the default no-op constraint evaluator was used" }
      return ConstraintEvaluation.valid(value)
   }
}
