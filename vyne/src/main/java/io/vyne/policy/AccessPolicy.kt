package io.vyne.policy


data class AuthorizationPolicy(
   val id: String,
   val policy: String
)

data class AuthorizationPolicyEvaluation(val decision: AuthorizationDecision, val message: String? = null)

enum class AuthorizationDecision {
   PERMITTED,
   PROCESS,
   DENIED
}
