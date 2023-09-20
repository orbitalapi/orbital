package com.orbitalhq.auth.authorisation

interface VyneUserRoleDefinitionRepository {
   fun findByRoleName(vyneRole: UserRole): VyneUserAuthorisationRoleDefinition?
   fun findAll(): Map<UserRole, VyneUserAuthorisationRoleDefinition>
   fun defaultUserRoles(): VyneDefaultUserRoleMappings
   fun defaultApiClientUserRoles(): VyneDefaultUserRoleMappings
}
