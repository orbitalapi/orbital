package com.orbitalhq.cockpit.core.security.authentication.oidc.propelAuth

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class PropelAuthUser(
   val userId: String,
   val email: String,
   val emailConfirmed: Boolean,
   val username: String?,
   val firstName: String,
   val lastName: String,
   val pictureUrl: String?,
   val properties: Map<String, Any> = emptyMap(),
   val hasPassword: Boolean,
   val updatePasswordRequired: Boolean,
   val locked: Boolean,
   val enabled: Boolean,
   val mfaEnabled: Boolean,
   val canCreateOrgs: Boolean,
   val createdAt: Long,
   val lastActiveAt: Long,
   val orgIdToOrgInfo: Map<String, OrgInfo> = emptyMap(),
   val legacyUserId: String? = null,
   val metadata: Any? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OrgInfo(
   val orgId: String,
   val orgName: String,
   val orgMetadata: Map<String, Any> = emptyMap(),
   val urlSafeOrgName: String,
   val userRole: String,
   val inheritedUserRolesPlusCurrentRole: List<String>,
   val userPermissions: List<String> = emptyList(),
   val orgRoleStructure: String,
   val additionalRoles: List<String> = emptyList()
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class PropelAuthApiKeyValidationResponse(
   val metadata: Map<String,Any>?,
   // Only present if the Api key is a users personal key
   // We could/should type this as a PropelAuthUser.
   // However, we need to present this as a set of claims in a fake JWT
   // to convert teh API key auth flow to a JWT based flow.
   // So, to save unnesseccary serde, just leave it as a user object
   val user: Map<String,Any>?,
   // Only present if the provided key is in an org
   val org: Map<String,Any>?,
   // Only present if the provided key is both a user and an API key
   val userInOrg: Map<String,Any>?,
)
