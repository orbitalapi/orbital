package com.orbitalhq.models.constraints

import com.orbitalhq.models.InPlaceQueryEngine
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.ConstraintEvaluation
import lang.taxi.services.operations.constraints.Constraint

data class DefaultConstraintEvaluator(
   val context: InPlaceQueryEngine
) : ConstraintEvaluator {
   override fun evaluate(constraint: Constraint, value: TypedInstance): ConstraintEvaluation {
      return constraint.evaluate(value, context.schema, context)
   }
}
