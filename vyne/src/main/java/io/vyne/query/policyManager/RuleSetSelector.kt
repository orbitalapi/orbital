package io.vyne.query.policyManager

import lang.taxi.policies.RuleSet

class RuleSetSelector() {
   fun select(executionScope: ExecutionScope, ruleSets: List<RuleSet>): RuleSet {
      require(ruleSets.isNotEmpty()) { "RuleSets must not be empty" }
      val (_, ruleSet) = ruleSets.map { ruleSet ->
         score(executionScope, ruleSet) to ruleSet
      }.sortedBy { it.first }
         .last()
      return ruleSet
   }

   private fun score(executionScope: ExecutionScope, ruleSet: RuleSet): Int {
      val ruleSetScope = ruleSet.scope
      val operationTypeScore = when {
         ruleSetScope.operationType == executionScope.operationType -> 1
         else -> 0
      }
      val scopeScore = when {
         ruleSetScope.operationScope == executionScope.operationScope -> 1
         else -> 0
      }
      return operationTypeScore + scopeScore
   }
}
