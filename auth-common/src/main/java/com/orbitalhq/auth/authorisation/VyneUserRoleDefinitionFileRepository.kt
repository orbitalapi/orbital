package com.orbitalhq.auth.authorisation

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import com.orbitalhq.config.BaseHoconConfigFileRepository
import java.nio.file.Path

class VyneUserRoleDefinitionFileRepository(
   path: Path,
   fallback: Config = ConfigFactory.systemEnvironment()
) :
   VyneUserRoleDefinitionRepository,
   BaseHoconConfigFileRepository<VyneUserAuthorisationRoleDefinitions>(path, fallback) {
   override fun findByRoleName(vyneRole: UserRole): VyneUserAuthorisationRoleDefinition? {
      val config = typedConfig()
      return config.grantedAuthorityMappings[vyneRole]
   }

   override fun findAll(): Map<UserRole, VyneUserAuthorisationRoleDefinition> {
      return typedConfig().grantedAuthorityMappings.mapKeys { it.key }
   }

   override fun defaultUserRoles(): VyneDefaultUserRoleMappings {
      return typedConfig().defaultUserRoleMappings
   }

   override fun defaultApiClientUserRoles(): VyneDefaultUserRoleMappings {
     return typedConfig().defaultApiClientRoleMappings
   }

   override fun extract(config: Config): VyneUserAuthorisationRoleDefinitions = config.extract()

   override fun emptyConfig(): VyneUserAuthorisationRoleDefinitions = VyneUserAuthorisationRoleDefinitions()
}
