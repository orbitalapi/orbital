package io.vyne.auth.authorisation

interface VyneUserRoleDefinitionRepository {
   fun findByRoleName(vyneRole: VyneUserAuthorisationRole): VyneUserAuthorisationRoleDefinition?
   fun findAll(): Map<VyneUserAuthorisationRole, VyneUserAuthorisationRoleDefinition>
   fun defaultUserRoles(): VyneDefaultUserRoleMappings
   fun defaultApiClientUserRoles(): VyneDefaultUserRoleMappings
}
