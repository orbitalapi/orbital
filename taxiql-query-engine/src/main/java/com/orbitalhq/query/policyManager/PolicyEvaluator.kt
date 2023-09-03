package com.orbitalhq.query.policyManager

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContext
import com.orbitalhq.schemas.Policy
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.log
import lang.taxi.policies.Instruction
import lang.taxi.policies.PermitInstruction
import lang.taxi.policies.PolicyOperationScope
import lang.taxi.services.OperationScope

/**
 * Similar to a policy scope, exception the operationType may not have been defined.
 * Current thinking is that operationScope should always be inferrable, as the engine
 * knows if we're doing an internal or external call.
 */
data class ExecutionScope(val operationType: OperationScope, val policyOperationScope: PolicyOperationScope)

class PolicyEvaluator(private val statementEvaluator: PolicyStatementEvaluator = PolicyStatementEvaluator(), private val defaultInstruction: Instruction = PermitInstruction) {

   fun evaluate(instance: TypedInstance, context: QueryContext, operationScope: ExecutionScope): Instruction {
      val schema = context.schema
      val policyType = getPolicyType(instance, context)
      val policies = findPolicies(schema, policyType)
      val instructions = policies.map { evaluate(it, instance, context, operationScope) }
      return when {
         instructions.isEmpty() -> defaultInstruction
         instructions.size == 1 -> instructions.first()
         else -> TODO("Multiple resulting instructions not yet supported")
      }
   }

   // This is kindda a hack
   // When a TypedCollection is passed in, it reports it's type as Foo, rather than Foo[].
   // This works well in other situations, but we want to find policies for the collection type,
   // not type member type.
   private fun getPolicyType(instance: TypedInstance, context: QueryContext): Type {
      return when (instance) {
         is TypedCollection -> instance.parameterizedType(context.schema)
         else -> instance.type
      }
   }

   private fun evaluate(policy: Policy, instance: TypedInstance, context: QueryContext, executionScope: ExecutionScope): Instruction {
      log().debug("Evaluating policy ${policy.name.fullyQualifiedName} for executionScope $executionScope")
      val ruleSets = policy.ruleSets
         //.filter { policy -> policy.scope.appliesTo(executionScope.operationType, executionScope.operationScope) }
      if (ruleSets.isEmpty()) {
         log().debug("No ruleset found for policy ${policy.name.fullyQualifiedName} with executionScope of $executionScope, so using default instruction of $defaultInstruction")
         return defaultInstruction
      }
      val ruleSet = RuleSetSelector().select(executionScope, ruleSets)
      //return context.startChild(this, "Evaluate policy ${policy.name} ruleSet ${ruleSet.scope}", OperationType.POLICY_EVALUATION) {

         val statementInstruction = statementEvaluator.evaluate(ruleSet, instance, context)
         return if (statementInstruction == null) {
            log().debug("Finished evaluating policy ${policy.name.fullyQualifiedName} for executionScope of $executionScope - no instruction matched, so using default instruction of $defaultInstruction")
            context.addAppliedInstruction(policy, defaultInstruction)
            defaultInstruction
         } else {
            log().debug("Finished evaluating policy ${policy.name.fullyQualifiedName} for executionScope of $executionScope - matched to instruction of $statementInstruction")
            context.addAppliedInstruction(policy, statementInstruction)
            statementInstruction
         }

      //}
   }


   // Design note:  Originally, here we looked at the raw type, so that
   // policies defined to Foo were also applied to Foo[]
   // However, since we now recurse through collections, this has been simplified
   // such that we only look at the type the policy is defined against.
   // This seems like a better approach, and we can enrich the recursion / introspection
   // process as required.  Review if this becomes untrue.
   private fun findPolicies(schema: Schema, type: Type): List<Policy> {
      return (listOf(schema.policy(type)) /* + type.typeParameters.map { schema.policy(it) } */).filterNotNull()
   }
}

data class PolicyEvaluationResult(
   val instruction: Instruction
)
