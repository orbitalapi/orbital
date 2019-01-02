package io.vyne.schemas

import lang.taxi.policies.RuleSet

/**
 * A policy against a specific data type, defining how it may be read or written.
 *
 * Note that these policies do not intend to restrict access to services,
 * (though that may occur as a side-effect),
 * instead to define how data is treated once it is retrieved, and before it
 * is passed downstream.
 */
data class Policy(
   val name: QualifiedName,
   val targetType: Type,
   val ruleSets: List<RuleSet>
)

