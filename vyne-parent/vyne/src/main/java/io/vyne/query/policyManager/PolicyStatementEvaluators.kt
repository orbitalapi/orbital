package io.vyne.query.policyManager

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import lang.taxi.policies.*


class PolicyStatementEvaluator(private val evaluators: List<ConditionalPolicyStatementEvaluator> = defaultEvaluators) {

   companion object {
      private val defaultEvaluators = listOf(ElseConditionEvaluator(), CaseConditionEvaluator())
   }

   private fun evaluate(statement: PolicyStatement, context: QueryContext): Instruction? {
      return evaluators.first { it.canSupport(statement.condition) }.evaluate(statement, context)
   }

   fun evaluate(ruleset: RuleSet, context: QueryContext): Instruction? {
      val instruction = ruleset.statements
         .asSequence()
         .map { evaluate(it, context) }
         .takeWhile { it == null }
         .last()
      TODO()
   }
}

interface ConditionalPolicyStatementEvaluator {
   fun canSupport(condition: Condition): Boolean
   fun evaluate(statement: PolicyStatement, context: QueryContext): Instruction?
}

class ElseConditionEvaluator : ConditionalPolicyStatementEvaluator {
   override fun canSupport(condition: Condition): Boolean = condition is ElseCondition

   override fun evaluate(statement: PolicyStatement, context: QueryContext): Instruction? {
      return statement.instruction
   }

}

class CaseConditionEvaluator : ConditionalPolicyStatementEvaluator {
   override fun canSupport(condition: Condition) = condition is CaseCondition

   override fun evaluate(statement: PolicyStatement, context: QueryContext): Instruction? {
      val condition = statement.condition as CaseCondition
      val lhValue = resolve(condition.lhSubject, context)
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   private fun resolve(subject: Subject, context: QueryContext): Any {
      return when (subject) {
         is RelativeSubject -> resolve(subject, context)
         else -> TODO()
      }
   }

   private fun resolve(subject: RelativeSubject, context: QueryContext): TypedInstance {
      val factSet = when (subject.source) {
         RelativeSubject.RelativeSubjectSource.CALLER -> context.callerFacts
         RelativeSubject.RelativeSubjectSource.THIS -> context.facts
      }

      val result = context.queryEngine.find(subject.targetType.qualifiedName, factSet)
      if (result.isFullyResolved) {
         return result[subject.targetType.qualifiedName]!!
      } else {
         TODO()
      }
   }

}
