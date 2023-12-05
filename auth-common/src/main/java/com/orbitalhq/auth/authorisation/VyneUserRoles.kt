package com.orbitalhq.auth.authorisation

import com.orbitalhq.auth.authentication.UserDisplayName
import com.orbitalhq.security.VyneGrantedAuthority
import java.util.concurrent.ConcurrentHashMap


typealias UserRole = String

enum class VyneConsumerType {
   USER,
   API
}
data class VyneUserRoles(val roles: Set<UserRole>, val type: String = VyneConsumerType.USER.name)


data class VyneUserRoleMappings(
   val userRoleMappings: MutableMap<UserDisplayName, VyneUserRoles> = ConcurrentHashMap()
)

/**
 * When user doesn't have a role, give these default role mappings.
 */
data class VyneDefaultUserRoleMappings(val roles: Set<UserRole> = emptySet())
data class VyneUserAuthorisationRoleDefinition(val grantedAuthorities: Set<VyneGrantedAuthority>)
data class VyneUserAuthorisationRoleDefinitions(
   val defaultUserRoleMappings: VyneDefaultUserRoleMappings = VyneDefaultUserRoleMappings(),
   val defaultApiClientRoleMappings: VyneDefaultUserRoleMappings = VyneDefaultUserRoleMappings(),
   // Ideally Key of this map should be VyneUserAuthorisationRole. but Hocon can't cope with Enum keys.
   val grantedAuthorityMappings: MutableMap<String, VyneUserAuthorisationRoleDefinition> = ConcurrentHashMap()
)


