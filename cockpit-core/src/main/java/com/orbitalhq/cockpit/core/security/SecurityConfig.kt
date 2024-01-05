package com.orbitalhq.cockpit.core.security

import com.orbitalhq.auth.CookieOrHeaderTokenConverter
import com.orbitalhq.auth.authentication.ConfigFileVyneUserRepository
import com.orbitalhq.auth.authentication.JwtStandardClaims
import com.orbitalhq.auth.authentication.VyneUserRepository
import com.orbitalhq.auth.authorisation.*
import com.orbitalhq.cockpit.core.lsp.LanguageServerConfig
import com.orbitalhq.cockpit.core.security.authorisation.JwtRolesExtractor
import com.orbitalhq.cockpit.core.security.authorisation.VyneAuthorisationConfig
import com.orbitalhq.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

private val logger = KotlinLogging.logger { }

@EnableWebFluxSecurity
@EnableConfigurationProperties(VyneAuthorisationConfig::class, VyneOpenIdpConnectConfig::class)
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


