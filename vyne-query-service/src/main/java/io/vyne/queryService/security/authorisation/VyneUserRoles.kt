package io.vyne.queryService.security.authorisation

import io.vyne.security.VyneGrantedAuthorities
import java.util.concurrent.ConcurrentHashMap

typealias VyneUserName = String
data class VyneUserRoles(val roles: Set<VyneUserAuthorisationRole>)
data class VyneUserRoleMappings(
   val userRoleMappings: MutableMap<VyneUserName, VyneUserRoles> = ConcurrentHashMap()
)

data class VyneUserAuthorisationRoleDefinition(val grantedAuthorities: Set<VyneGrantedAuthorities>)
data class VyneUserAuthorisationRoleDefinitions(
   // Ideally Key of this map should be VyneUserAuthorisationRole. but Hocon can't cope with Enum keys.
   val grantedAuthorityMappings: MutableMap<String, VyneUserAuthorisationRoleDefinition> = ConcurrentHashMap()
)

enum class VyneUserAuthorisationRole(val displayValue: String) {
   Admin("Admin"),
   Viewer("Viewer"),
   QueryRunner("Query Runner"),
   PlatformManager("Platform Manager")
}
