package com.orbitalhq.cockpit.core.security

import com.orbitalhq.auth.authorisation.VyneUserRoleDefinitionFileRepository
import com.orbitalhq.auth.authorisation.VyneUserRoleDefinitionRepository
import com.orbitalhq.cockpit.core.security.authorisation.VyneAuthorisationConfig
import com.orbitalhq.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import com.orbitalhq.cockpit.core.security.authorisation.VyneSamlConfig
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity

private val logger = KotlinLogging.logger { }

@EnableWebFluxSecurity
@EnableConfigurationProperties(
   VyneAuthorisationConfig::class,
   VyneOpenIdpConnectConfig::class,
   VyneSamlConfig::class)
@Configuration
class SecurityConfig {

   @Bean
   fun vyneUserRoleDefinitionRepository(authorisationConfig: VyneAuthorisationConfig): VyneUserRoleDefinitionRepository {
      if (!authorisationConfig.roleDefinitionsFile.toFile().exists()) {
         logger.info { "No role definition found at ${authorisationConfig.roleDefinitionsFile.toFile().canonicalPath}. Creating a default file." }
         authorisationConfig.roleDefinitionsFile.toFile().parentFile.mkdirs()
         IOUtils.copy(
            ClassPathResource("authorisation/vyne-authorisation-role-definitions.conf").inputStream,
            authorisationConfig.roleDefinitionsFile.toFile().outputStream()
         )
         logger.info { "Default role definition written to ${authorisationConfig.roleDefinitionsFile.toFile().canonicalPath}." }
      } else {
         logger.info { "Using role definition at ${authorisationConfig.roleDefinitionsFile.toFile().canonicalPath}." }
      }
      return VyneUserRoleDefinitionFileRepository(path = authorisationConfig.roleDefinitionsFile)
   }
}


