package io.vyne.cockpit.core.security

import io.vyne.auth.CookieOrHeaderTokenConverter
import io.vyne.auth.authorisation.VyneUserRoleDefinitionFileRepository
import io.vyne.auth.authorisation.VyneUserRoleDefinitionRepository
import io.vyne.auth.authorisation.VyneUserRoleMappingFileRepository
import io.vyne.auth.authorisation.VyneUserRoleMappingRepository
import io.vyne.cockpit.core.lsp.LanguageServerConfig
import io.vyne.cockpit.core.security.authorisation.VyneAuthorisationConfig
import io.vyne.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
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
   fun vyneUserRoleMappingRepository(authorisationConfig: VyneAuthorisationConfig): VyneUserRoleMappingRepository {
      return VyneUserRoleMappingFileRepository(path = authorisationConfig.userToRoleMappingsFile)
   }

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

   @ConditionalOnProperty("vyne.security.openIdp.enabled", havingValue = "false", matchIfMissing = true)
   @Configuration
   class UnsecureConfig {
      @Bean
      fun springWebFilterChainNoAuthentication(http: ServerHttpSecurity): SecurityWebFilterChain? {
         return http
            .csrf().disable()
            .cors().disable()
            .headers().disable()
            .authorizeExchange()
            .anyExchange().permitAll()
            .and()
            .build()
      }
   }

   @ConditionalOnProperty("vyne.security.openIdp.enabled", havingValue = "true", matchIfMissing = false)
   @Configuration
   // useAuthorizationManager = false required for support for method based PreAuthorize when returning a Kotlin Flow
// see: https://github.com/spring-projects/spring-security/issues/12821
   @EnableReactiveMethodSecurity(useAuthorizationManager = false)

   class VyneReactiveSecurityConfig {
      @Bean
      fun grantedAuthoritiesExtractor(
         vyneAuthorisationConfig: VyneAuthorisationConfig,
         vyneUserRoleMappingRepository: VyneUserRoleMappingRepository,
         vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository
      ): GrantedAuthoritiesExtractor {
         return GrantedAuthoritiesExtractor(
            vyneUserRoleMappingRepository,
            vyneUserRoleDefinitionRepository,
            vyneAuthorisationConfig.adminRole
         )
      }

      @Bean
      fun springWebFilterChain(
         http: ServerHttpSecurity,
         languageServerConfig: LanguageServerConfig,
         @Value("\${management.endpoints.web.base-path:/actuator}") actuatorPath: String,
         grantedAuthoritiesExtractor: GrantedAuthoritiesExtractor
      ): SecurityWebFilterChain {
         http
            .securityMatcher {
               NegatedServerWebExchangeMatcher(
                  ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/api/security/config")
               ).matches(it)
            }
            .csrf().disable()
            .cors().configurationSource(
               UrlBasedCorsConfigurationSource().let { configSrc ->
                  val config = CorsConfiguration()
                  config.addAllowedOrigin("*")
                  config.addAllowedHeader("*")
                  config.addExposedHeader("*")
                  config.addAllowedMethod("")
                  configSrc.registerCorsConfiguration("/**", config)
                  configSrc
               }
            ).and()
            .headers().frameOptions().mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN).and()
            .authorizeExchange()
            // End points for Cask and other vyne based services to fetch the schema in EUREKA schema discovery mode.
            .pathMatchers("/api/security/config").permitAll()
            // All other api end points must be protected.
            .pathMatchers("/api/**").authenticated()
            .pathMatchers(
               "/**", // Allow access to any, to support html5 ui routes (eg /types/foo.bar.Baz)
               "/assets/**",
               "/index.html",
               actuatorPath,
               languageServerConfig.path,
               "/*.js",
               "/*.css"
            ).permitAll()
            .anyExchange().authenticated()
            .and()
            // Below sets up the Vyne as a resource server, so Vyne will check if there is an 'access' token on every request
            // and whether the token is valid or not. In order to verify that a token is genuine, it'll talk to openID connect server
            // (see spring.security.oauth2.resourceserver.jwt.jwk-set-uri)
            .oauth2ResourceServer()
            .bearerTokenConverter(CookieOrHeaderTokenConverter())
            .jwt()
            // Below we populate set of GrantedAuthorities for the user.
            .jwtAuthenticationConverter { jwt ->
               val jwtAuthenticationConverter = JwtAuthenticationConverter()
               jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesExtractor)
               ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter).convert(jwt)
            }
         return http.build()
      }
   }

}


