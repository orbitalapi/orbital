package io.vyne.query.policyManager

import io.vyne.query.QueryContext
import io.vyne.query.QuerySpecTypeNode
import io.vyne.schemas.Policy
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.policies.Instruction
import lang.taxi.policies.OperationScope

/**
 * Similar to a policy scope, exception the operationType may not have been defined.
 * Current thinking is that operationScope should always be inferrable, as the engine
 * knows if we're doing an internal or external call.
 */
data class ExecutionScope(val operationType: String?, val operationScope: OperationScope)

class PolicyEvaluator(private val statementEvaluator: PolicyStatementEvaluator = PolicyStatementEvaluator(), private val defaultInstruction: Instruction = Instruction(Instruction.InstructionType.PERMIT)) {


   fun evaluate(target: Set<QuerySpecTypeNode>, context: QueryContext): PolicyEvaluationResult {
//      val schema = context.schema
//      val instructions = target.flatMap { spec ->
//         val policies = findPolicies(schema, spec.type)
//         policies.map { evaluate(it, context) }
//      }
      TODO()
   }

   fun evaluate(type: Type, context: QueryContext, operationScope: ExecutionScope): Instruction {
      val schema = context.schema
      val policies = findPolicies(schema, type)
      val instructions = policies.map { evaluate(it, context, operationScope) }
      return when {
         instructions.isEmpty() -> defaultInstruction
         instructions.size == 1 -> instructions.first()
         else -> TODO("Multiple resulting instructions not yet supported")
      }
   }

   private fun evaluate(policy: Policy, context: QueryContext, executionScope: ExecutionScope): Instruction {
      val ruleSets = policy.ruleSets.filter { policy -> policy.scope.appliesTo(executionScope.operationType,executionScope.operationScope) }
      if (ruleSets.isEmpty()) {
         return defaultInstruction
      }
      val ruleSet = RuleSetSelector().select(executionScope, ruleSets)
      return statementEvaluator.evaluate(ruleSet, context) ?: defaultInstruction
   }


   private fun findPolicies(schema: Schema, type: Type): List<Policy> {
      return (listOf(schema.policy(type)) + type.typeParameters.map { schema.policy(it) }).filterNotNull()
   }
}

data class PolicyEvaluationResult(
   val instruction: Instruction
)

