package io.vyne.queryService.security.authorisation

import io.vyne.security.VyneGrantedAuthorities
import java.util.concurrent.ConcurrentHashMap

typealias VyneUserName = String
typealias VyneUserAuthorisationRole = String

enum class VyneConsumerType {
   USER,
   API
}
data class VyneUserRoles(val roles: Set<VyneUserAuthorisationRole>, val type: String = VyneConsumerType.USER.name)
data class VyneUserRoleMappings(
   val userRoleMappings: MutableMap<VyneUserName, VyneUserRoles> = ConcurrentHashMap()
)

/**
 * When user doesn't have a role, give these default role mappings.
 */
data class VyneDefaultUserRoleMappings(val roles: Set<VyneUserAuthorisationRole> = emptySet())
data class VyneUserAuthorisationRoleDefinition(val grantedAuthorities: Set<VyneGrantedAuthorities>)
data class VyneUserAuthorisationRoleDefinitions(
   val defaultUserRoleMappings: VyneDefaultUserRoleMappings = VyneDefaultUserRoleMappings(),
   val defaultApiClientRoleMappings: VyneDefaultUserRoleMappings = VyneDefaultUserRoleMappings(),
   // Ideally Key of this map should be VyneUserAuthorisationRole. but Hocon can't cope with Enum keys.
   val grantedAuthorityMappings: MutableMap<String, VyneUserAuthorisationRoleDefinition> = ConcurrentHashMap()
)


