package com.orbitalhq.query.policyManager

import lang.taxi.policies.PolicyRule

object RuleSetSelector {
   // This used to be more complex, involving scoring.
   // Have simplified it, unless there's a use-case.
   // Check the history before this comment for the old implementation
   fun select(executionScope: ExecutionScope, ruleSets: List<PolicyRule>): PolicyRule? {
      return ruleSets.firstOrNull { executionScope.matches(it) }
   }

}
