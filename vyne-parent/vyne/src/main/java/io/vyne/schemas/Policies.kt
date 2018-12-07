package io.vyne.schemas

import lang.taxi.policies.RuleSet

data class Policy(
   val name: QualifiedName,
   val targetType: Type,
   val ruleSets: List<RuleSet>
)

