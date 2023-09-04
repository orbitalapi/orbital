package com.orbitalhq.query.policyManager

import lang.taxi.policies.PolicyScope
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

         ruleSetScope.operationType == PolicyScope.WILDCARD_OPERATION_TYPE -> 1
         ruleSetScope.operationType == executionScope.operationType.token -> 1
         ruleSetScope.operationType == executionScope.operationType.name -> 1
         else -> 0
      }
      val scopeScore = when {
         ruleSetScope.policyOperationScope == executionScope.policyOperationScope -> 1
         else -> 0
      }
      return operationTypeScore + scopeScore
   }
}
