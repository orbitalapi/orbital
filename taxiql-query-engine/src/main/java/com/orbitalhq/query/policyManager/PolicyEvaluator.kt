package com.orbitalhq.query.policyManager

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.query.QueryContext
import com.orbitalhq.models.ProjectionFunctionScopeEvaluator
import com.orbitalhq.models.TypedInstanceConverter
import com.orbitalhq.models.TypedInstanceMapper
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.PolicyWithPath
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
   fun matches(ruleSet: PolicyRule): Boolean {
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

class PolicyEvaluator {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun evaluate(instance: TypedInstance, context: QueryContext, operationScope: ExecutionScope): TypedInstance {
      val schema = context.schema
      val policyType = getPolicyType(instance, context)
      val policies = findPolicies(schema, policyType)
      // bail early
      val evaluationResult = if (policies.isEmpty()) {
         return instance
      } else {
         evaluate(policies, instance, context, operationScope)
      }
      return evaluationResult
   }

   private fun evaluate(
      applicablePolicies: List<PolicyWithPath>,
      instance: TypedInstance,
      context: QueryContext,
      operationScope: ExecutionScope
   ): TypedInstance {
      val updates = applicablePolicies.flatMap { policyWithPath ->
         evaluatePolicyAtPath(policyWithPath, instance, context, operationScope)
      }
      if (updates.isEmpty()) {
         return instance
      }

      val updatesBySourceValue = updates.groupBy { it.sourceInstance }

      // We've collected all the mutations to our TypedInstance that are the result of policy evaluations.
      // We now need to apply them.
      // We use a TypedInstanceConverter - however this doesn't apply conversions on the root object, only
      // on the attributes.
      // Therefore, we need to manually apply conversions on the root first.
      val updatedToRoot = updatesBySourceValue.getOrDefault(instance, emptyList())
      val updatedRootInstance = MutatingTypedInstanceMapper.applyMutationsToInstance(instance, updatedToRoot)

      val updatedRaw = TypedInstanceConverter(MutatingTypedInstanceMapper(updatesBySourceValue)).convert(updatedRootInstance)

      // Now rebuild the typed instance.
      // The MutatingTypedInstanceMapper omits any fields that are the result of evaluation (because the input to the
      // evluation may have been a sensitive value, or been changed by a policy).
      // By reconstructing the object, expressions are re-evaluated.
      // Note that the type we use isn't the original type, but the type of the new
      // root object. This is because a policy may have changed the object (eg., by
      // using a spread operator to drop off fields).
      // Otherwise, if we use the old type, we get those fields back, populated as null.
      val newValue = TypedInstance.from(updatedRootInstance.type, updatedRaw, context.schema, source = instance.source)
      return newValue
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

   private fun evaluatePolicyAtPath(
      policyWithPath: PolicyWithPath,
      rootInstance: TypedInstance,
      context: QueryContext,
      executionScope: ExecutionScope
   ): List<PolicyResultMutation> {
      val policy = policyWithPath.policy
      val policyRule = RuleSetSelector.select(executionScope, policy.rules)
         ?: return emptyList()
      logger.debug { "Evaluating policy ${policy.qualifiedName} for executionScope $executionScope" }

      return if (policyWithPath.path.isEmpty()) {
         val updated = evaluatePolicy(policy, policyRule, rootInstance, context)
         listOfNotNull(PolicyResultMutation.ifDifferent(policyWithPath.path, rootInstance, updated, policy, policyRule))
      } else {
         require(rootInstance is TypedObject) { "Cannot apply policy ${policy.qualifiedName} as provided input of type ${rootInstance.typeName} is not a TypedObject" }
         val instancesToApplyPolicyTo = rootInstance.getAllAtPath(policyWithPath.path)
         val mutations = instancesToApplyPolicyTo.mapNotNull { instance ->
            val updatedValue = evaluatePolicy(policy, policyRule, instance, context)
            PolicyResultMutation.ifDifferent(policyWithPath.path, instance, updatedValue, policy, policyRule)
         }
         mutations
      }
   }

   private fun evaluatePolicy(
      policy: Policy,
      rule: PolicyRule,
      instance: TypedInstance,
      context: QueryContext,
   ): TypedInstance {
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
         .evaluate(rule.expression, facts)
      return evaluationResult
   }


   @VisibleForTesting
   internal fun findPolicies(schema: Schema, type: Type): List<PolicyWithPath> {
      return schema.findPoliciesForTypeAndDescendants(type)
   }

   private data class PolicyResultMutation(
      val path: String, // I don't think we'll need this
      val sourceInstance: TypedInstance,
      val updatedInstance: TypedInstance,
      val policy: Policy,
      val rule: PolicyRule
   ) {
      companion object {
         fun ifDifferent(
            path: String,
            sourceInstance: TypedInstance,
            updatedInstance: TypedInstance,
            policy: Policy,
            rule: PolicyRule
         ): PolicyResultMutation? {
            return if (sourceInstance != updatedInstance) {
               val updatedDataSource = ModifiedByDataPolicySource(
                  policy.qualifiedName,
                  rule.operationScope,
                  rule.policyScope,
                  updatedInstance.source
               )
               val updatedInstanceWithNewDataSource = DataSourceUpdater.update(updatedInstance, updatedDataSource)
               PolicyResultMutation(path, sourceInstance, updatedInstanceWithNewDataSource, policy, rule)
            } else null
         }
      }
   }

   private class MutatingTypedInstanceMapper(private val mutations: Map<TypedInstance, List<PolicyResultMutation>>) :
      TypedInstanceMapper {

      companion object {
         fun applyMutationsToInstance(
            typedInstance: TypedInstance,
            mutations: List<PolicyResultMutation>
         ): TypedInstance {
            // TODO : We should be ordering the mutations by order on the policy
            // I've included the policy in the mutation so that we can do that.
            // But need to add some mechansim (probably an annotation on the policy)
            // for expressing order. For now, just log.
            return when {
               mutations.isEmpty() -> typedInstance
               mutations.size == 1 -> mutations.single().updatedInstance
               else -> {
                  val mutationToApply = mutations.last()
                  logger.warn { "Multiple polices defined mutations for type ${typedInstance.type.paramaterizedName} - ${mutations.joinToString { it.policy.qualifiedName }} - however ordering is not yet implemented. Applying the last policy - ${mutationToApply.policy.qualifiedName}" }
                  mutationToApply.updatedInstance
               }
            }
         }
      }

      override fun map(typedInstance: TypedInstance): Any? {
         return when {
            typedInstance.source is EvaluatedExpression -> null
            mutations.containsKey(typedInstance) -> {
               val mutations = mutations.getValue(typedInstance)
               applyMutationsToInstance(typedInstance, mutations)
            }

            else -> typedInstance
         }
      }
   }
}


class ModifiedByDataPolicySource(
   val policyName: String,
   val operationScope: OperationScope?,
   val policyScope: PolicyOperationScope?,
   val valueDataSource: DataSource
) : DataSource {
   override val name: String = "ModifiedByPolicy"
   override val id: String = this.hashCode().toString()
   override val failedAttempts: List<DataSource> = emptyList()
}
