package io.vyne.auth.authorisation

import io.vyne.auth.authentication.UserDisplayName

@Deprecated(message = "Use JPA instead", level = DeprecationLevel.WARNING)
interface VyneUserRoleMappingRepository {
   fun findByUserName(userName: String): VyneUserRoles?
   fun save(userName: String, roleMapping: VyneUserRoles): VyneUserRoles

   fun findAll(): Map<UserDisplayName, VyneUserRoles>
   fun deleteByUserName(userName: String)
   fun size(): Int
}
