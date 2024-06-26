package com.orbitalhq.query.policyManager

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.query.QueryContext
import com.orbitalhq.models.ProjectionFunctionScopeEvaluator
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.policies.Policy
import lang.taxi.policies.PolicyOperationScope
import lang.taxi.policies.PolicyRule
import lang.taxi.services.OperationScope
import mu.KotlinLogging

/**
 * Similar to a policy scope, exception the operationType may not have been defined.
 * Current thinking is that operationScope should always be inferrable, as the engine
 * knows if we're doing an internal or external call.
 */
data class ExecutionScope(val operationScope: OperationScope, val policyOperationScope: PolicyOperationScope) {
   fun matches(ruleSet: PolicyRule):Boolean {
      val operationScopeMatches = when {
         ruleSet.operationScope == null -> true
         else -> ruleSet.operationScope == operationScope
      }
      val policyScopeMatches = when {
         ruleSet.policyScope == null -> true
         else -> ruleSet.policyScope == policyOperationScope
      }
      return operationScopeMatches && policyScopeMatches
   }
}

class PolicyEvaluator() {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun evaluate(instance: TypedInstance, context: QueryContext, operationScope: ExecutionScope): TypedInstance {
      val schema = context.schema
      val policyType = getPolicyType(instance, context)
      val policies = findPolicies(schema, policyType)
      return when {
         policies.isEmpty() -> instance
         policies.size == 1 -> {
            evaluate(policies.single(), instance, context, operationScope)
         }
         else -> TODO("Multiple resulting instructions not yet supported")
      }
   }

   // This is kinda a hack
   // When a TypedCollection is passed in, it reports it's type as Foo, rather than Foo[].
   // This works well in other situations, but we want to find policies for the collection type,
   // not type member type.
   private fun getPolicyType(instance: TypedInstance, context: QueryContext): Type {
      return when (instance) {
         is TypedCollection -> instance.parameterizedType(context.schema)
         else -> instance.type
      }
   }

   private fun evaluate(policy: Policy, instance: TypedInstance, context: QueryContext, executionScope: ExecutionScope): TypedInstance {
      val ruleSet = RuleSetSelector.select(executionScope, policy.rules)
         ?: return instance
      logger.debug { "Evaluating policy ${policy.qualifiedName} for executionScope $executionScope" }

      val inputs = ProjectionFunctionScopeEvaluator.build(
         policy.inputs,
         // Important: Add the fact to a new version of the context's
         // fact bag, otherwise we end up in a recursive loop
         // where the inputs aren't available, so we do a search,
         // triggering a service call, which applies the policy, which hits this method, etc etc
         context.facts.addFact(instance).rootAndScopedFacts(),
         context
      )
      val facts = FactBag.of(instance, context.schema)
         .withAdditionalScopedFacts(inputs, context.schema)
      val evaluationResult = context
         .evaluate(ruleSet.expression, facts)
      return evaluationResult
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


