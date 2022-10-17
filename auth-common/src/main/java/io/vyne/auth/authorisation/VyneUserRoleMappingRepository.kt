package io.vyne.auth.authorisation

interface VyneUserRoleMappingRepository {
   fun findByUserName(userName: String): VyneUserRoles?
   fun save(userName: String, roleMapping: VyneUserRoles): VyneUserRoles

   fun findAll(): Map<VyneUserName, VyneUserRoles>
   fun deleteByUserName(userName: String)
   fun size(): Int
}
