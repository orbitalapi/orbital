package io.vyne.query.policyManager

import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedValue
import lang.taxi.Operator
import lang.taxi.policies.LiteralArraySubject

object OperatorEvaluators {

   private val evaluators: Map<Operator, OperatorEvaluator> = listOf(
      EqualOperatorEvaluator(),
      InOperatorEvaluator(),
      NotEqualOperatorEvaluator(EqualOperatorEvaluator())
   ).associateBy { it.operator }

   fun get(operator: Operator): OperatorEvaluator {
      return evaluators[operator] ?: error("No evaluator for operator ${operator.symbol}")
   }
}

interface OperatorEvaluator {
   val operator: Operator
   fun evaluate(lhSubject: Any?, rhSubject: Any?): Boolean
}

class NotEqualOperatorEvaluator(private val equalOperatorEvaluator: EqualOperatorEvaluator) : OperatorEvaluator {
   override val operator: Operator = Operator.NOT_EQUAL

   override fun evaluate(lhSubject: Any?, rhSubject: Any?): Boolean {
      return !equalOperatorEvaluator.evaluate(lhSubject, rhSubject)
   }

}

class InOperatorEvaluator : OperatorEvaluator {
   override val operator: Operator = Operator.IN
   override fun evaluate(lhSubject: Any?, rhSubject: Any?): Boolean {
      require(rhSubject is LiteralArraySubject) { "When using the 'in' operator, the right hand side must be an array of values.  (eg., ['a','b']" }
      val rhArraySubject: LiteralArraySubject = rhSubject as LiteralArraySubject
      val rhValues = rhArraySubject.values
      require(lhSubject is TypedInstance) { "When using the 'in' operator, the left hand side must have been resolved to a TypedInstance" }

      val valueCollection = when (val lhValue = lhSubject.value) {
         is Collection<*> -> lhValue
         else -> listOf(lhValue)
      }

      return valueCollection.any { rhValues.contains(it) }
   }
}

class EqualOperatorEvaluator : OperatorEvaluator {
   override fun evaluate(lhSubject: Any?, rhSubject: Any?): Boolean {
      return when {
         // For now, treating null as "special" here, but would be nice to find a way not to, as
         // I suspect our special cases could grow.
         lhSubject == null && rhSubject == null -> true
         lhSubject is TypedNull && rhSubject == null -> true
         lhSubject == null && rhSubject is TypedNull -> true
         lhSubject is TypedValue && rhSubject is TypedValue -> lhSubject.valueEquals(rhSubject)
         else -> lhSubject == rhSubject
      }
   }

   override val operator = Operator.EQUAL
}
