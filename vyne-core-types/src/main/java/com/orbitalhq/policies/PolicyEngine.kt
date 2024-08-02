package com.orbitalhq.policies

import com.orbitalhq.models.InPlaceQueryEngine
import com.orbitalhq.models.TypedInstance
import lang.taxi.policies.PolicyRule

interface PolicyEngine {
   fun evaluate(instance: TypedInstance, context: InPlaceQueryEngine, operationScope: PolicyExecutionScope): TypedInstance
}

interface PolicyExecutionScope {
   fun matches(ruleSet: PolicyRule):Boolean
}

/**
 * A wrapper for providing a policy engine with
 * a specific scope.
 *
 * Used when constructing an object -- where you need to know
 * more than just that the policy engine exists, but also
 * the conditions under which the object is being constructed
 */
interface ScopedPolicyEngine {
   val policyEngine: PolicyEngine
   val scope: PolicyExecutionScope

   fun evaluate(instance: TypedInstance, context: InPlaceQueryEngine) = policyEngine.evaluate(instance, context, scope)
}
