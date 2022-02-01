package io.vyne.queryService.security.authorisation

interface VyneUserRoleDefinitionRepository {
   fun findByRoleName(vyneRole: VyneUserAuthorisationRole): VyneUserAuthorisationRoleDefinition?
   fun findAll(): Map<VyneUserAuthorisationRole, VyneUserAuthorisationRoleDefinition>
}
