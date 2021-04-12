package io.vyne.query.policyManager

import io.vyne.FactSets
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.TypeNameQueryExpression
import io.vyne.utils.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import lang.taxi.policies.*


class PolicyStatementEvaluator(private val evaluators: List<ConditionalPolicyStatementEvaluator> = defaultEvaluators) {

   companion object {
      private val defaultEvaluators = listOf(ElseConditionEvaluator(), CaseConditionEvaluator())
   }

   private fun evaluate(statement: PolicyStatement, instance: TypedInstance, context: QueryContext): Instruction? {
      return evaluators.first { it.canSupport(statement.condition) }.evaluate(statement, instance, context)
   }

   fun evaluate(ruleset: RuleSet, instance: TypedInstance, context: QueryContext): Instruction? {

//      var matchedInstruction:Instruction? = null

      val instruction = ruleset.statements
         .asSequence()
         .map {

            log().warn("A blocking call is being made - policystatementevaluators.evaluate")
            val result = runBlocking { evaluate(it, instance, context) }
            if (result != null) {
               log().debug("Policy statement \"${it.source.source.content}\" matched with instruction ${result.toString()}")
            } else {
               log().debug("Policy statement \"${it.source.source.content}\" did not match")
            }
//            matchedInstruction = result
            result
         }
         .firstOrNull { it != null }

//         .takeWhile { it == null }
//         .last()
      return instruction
   }
}

interface ConditionalPolicyStatementEvaluator {
   fun canSupport(condition: Condition): Boolean
   fun evaluate(statement: PolicyStatement, instance: TypedInstance, context: QueryContext): Instruction?
}

class ElseConditionEvaluator : ConditionalPolicyStatementEvaluator {
   override fun canSupport(condition: Condition): Boolean = condition is ElseCondition

   override fun evaluate(statement: PolicyStatement, instance: TypedInstance, context: QueryContext): Instruction? {
      return statement.instruction
   }

}

class CaseConditionEvaluator : ConditionalPolicyStatementEvaluator {
   override fun canSupport(condition: Condition) = condition is CaseCondition

   override fun evaluate(statement: PolicyStatement, instance: TypedInstance, context: QueryContext): Instruction? {
      val condition = statement.condition as CaseCondition
      val lhValue = resolve(condition.lhSubject, instance, context)
      val rhValue = resolve(condition.rhSubject, instance, context)
      val evaluator = OperatorEvaluators.get(condition.operator)
      return if (evaluator.evaluate(lhValue, rhValue)) {
         statement.instruction
      } else {
         null
      }
   }

   private fun resolve(subject: Subject, instance: TypedInstance, context: QueryContext): Any? {
      return when (subject) {
         is RelativeSubject -> resolve(subject, instance, context)
         is LiteralArraySubject -> resolve(subject, context)
         is LiteralSubject -> resolve(subject, context)
         else -> TODO()
      }
   }

   private fun resolve(subject: LiteralSubject, context: QueryContext): Any? {
      return subject.value
   }

   private fun resolve(subject: LiteralArraySubject, context: QueryContext): Any {
      return subject
   }

   private fun resolve(subject: RelativeSubject, instance: TypedInstance, context: QueryContext): Flow<TypedInstance> {
      val contextToUse = when (subject.source) {
         RelativeSubject.RelativeSubjectSource.CALLER -> context.queryEngine.queryContext(setOf(FactSets.CALLER), queryId = context.queryId, clientQueryId = context.clientQueryId)
         // Use nothing from the context, except the current thing being filtered.
         RelativeSubject.RelativeSubjectSource.THIS -> context.queryEngine.queryContext(setOf(FactSets.NONE), additionalFacts = setOf(instance), queryId = context.queryId, clientQueryId = context.clientQueryId)
      }

      log().warn("A blocking call is being made policystatementevaluations resolve")
      val result = runBlocking { contextToUse.find(TypeNameQueryExpression(subject.targetType.qualifiedName)) }
      if (result.isFullyResolved) {
         return result.results!!
      } else {
         // TODO : Might want to consider something more sophisticated here.
         throw PolicyNotEvaluatableException("Could not find a path to evaluate ${subject.targetType.qualifiedName} relative to subjectSource ${subject.source}")
      }
   }

}

class PolicyNotEvaluatableException(message:String) : RuntimeException(message)
