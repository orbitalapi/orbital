package com.orbitalhq.query

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.constraints.ConstraintEvaluator
import com.orbitalhq.schemas.DefaultConstraintEvaluation
import com.orbitalhq.schemas.RemoteOperation
import lang.taxi.TaxiParser.QueryContext
import lang.taxi.services.operations.constraints.Constraint

/**
 * Interface to provide fine-grained control over
 * building of instances and determining if proposed instances
 * are suitable.
 *
 * Current use case is here to support the concept of annotations on target
 * types for things like FirstNotEmpty, without littering code everywhere.
 *
 * This is a sketch, and may not live long.
 *
 */
interface TypedInstanceValidPredicate {
   fun isValid(typedInstance: TypedInstance?): Boolean

}

/**
 * A spec that will accept anything.
 * This serves as the default.
 */
object AlwaysGoodSpec : TypedInstanceValidPredicate {
   override fun isValid(typedInstance: TypedInstance?) = true
}

data class InvocationConstraints(
   val typedInstanceValidPredicate: TypedInstanceValidPredicate,
   val excludedOperations: Set<SearchGraphExclusion<RemoteOperation>> = emptySet()
) {
   companion object {
      val withAlwaysGoodPredicate: InvocationConstraints = InvocationConstraints(AlwaysGoodSpec)
   }
}

data class SatisfiesConstraintsSpec(val evaluator: ConstraintEvaluator, val constraints: List<Constraint>) :
   TypedInstanceValidPredicate {
   override fun isValid(typedInstance: TypedInstance?): Boolean {
      if (typedInstance == null) return false
      return evaluator.allConstraintsAreValid(constraints, typedInstance)
   }
   companion object {
      fun forConstraints(evaluator: ConstraintEvaluator, constraints: List<Constraint>): TypedInstanceValidPredicate {
         return if (constraints.isEmpty()) {
            AlwaysGoodSpec
         } else {
            SatisfiesConstraintsSpec(evaluator, constraints)
         }

      }
   }
}

data class CompositeSpec(private val specs: List<TypedInstanceValidPredicate>) : TypedInstanceValidPredicate {
   override fun isValid(typedInstance: TypedInstance?): Boolean {
      return specs.all { it.isValid(typedInstance) }
   }
   companion object {
      fun combine(a:TypedInstanceValidPredicate, b:TypedInstanceValidPredicate): TypedInstanceValidPredicate {
         return when {
            a is AlwaysGoodSpec -> b
            b is AlwaysGoodSpec -> a
            else -> CompositeSpec(listOf(a,b))
         }
      }
   }
}
