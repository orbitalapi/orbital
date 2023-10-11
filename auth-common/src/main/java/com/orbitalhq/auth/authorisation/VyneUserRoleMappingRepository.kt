package com.orbitalhq.auth.authorisation

import com.orbitalhq.auth.authentication.UserDisplayName


@Deprecated(message = "Use JPA instead", level = DeprecationLevel.WARNING)
interface VyneUserRoleMappingRepository {
   fun findByUserName(userName: String): VyneUserRoles?
   fun save(userName: String, roleMapping: VyneUserRoles): VyneUserRoles

   fun findAll(): Map<UserDisplayName, VyneUserRoles>
   fun deleteByUserName(userName: String)
   fun size(): Int
}
