package io.orbital.station.security

import io.vyne.auth.CookieOrHeaderTokenConverter
import io.vyne.auth.authentication.ConfigFileVyneUserRepository
import io.vyne.auth.authentication.VyneUserRepository
import io.vyne.auth.authorisation.*
import io.orbital.station.lsp.LanguageServerConfig
import io.vyne.queryService.security.authorisation.VyneAuthorisationConfig
import io.vyne.queryService.security.authorisation.VyneOpenIdpConnectConfig
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

private val logger = KotlinLogging.logger { }

@EnableWebFluxSecurity
@EnableConfigurationProperties(VyneAuthorisationConfig::class, VyneOpenIdpConnectConfig::class)
class OrbitalSecurityAutoConfig {

   @Bean
   fun vyneUserRepository(config: OrbitalUserConfig): VyneUserRepository {
      return ConfigFileVyneUserRepository(config.configFile)
   }

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
            .headers().frameOptions().mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN).and()
            .authorizeExchange()
            // End points for Cask and other vyne based services to fetch the schema in EUREKA schema discovery mode.
            .pathMatchers("/api/security/config", "/api/schemas").permitAll()
            // All other api end points must be protected.
            .pathMatchers("/api/**").authenticated()
            .pathMatchers(
               "/**", // Allow access to any, to support html5 ui routes (eg /types/foo.bar.Baz)
               "/assets/**",
//               "/index.html",
               actuatorPath,
               languageServerConfig.path,
               "/*.js",
               "/*.css"
            ).permitAll()
            .anyExchange().authenticated()
//            .and().exceptionHandling()
//            .authenticationEntryPoint(authenticationFailureEntryPoint(http))
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

      fun authenticationFailureEntryPoint(http: ServerHttpSecurity): ServerAuthenticationEntryPoint {
         return ServerAuthenticationEntryPoint { exchange, ex ->
            if (exchange.request.path.value().startsWith("/api")) {
               HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
                  .commence(exchange, ex)
            } else {
               RedirectServerAuthenticationEntryPoint("//www.google.com")
                  .commence(exchange, ex)
            }
         }
      }

   }


}


/**
 * Converts given jwt token to set of granted authorities.
 */
class GrantedAuthoritiesExtractor(
   private val vyneUserRoleMappingRepository: VyneUserRoleMappingRepository,
   vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository,
   private val adminRoleName: VyneUserAuthorisationRole
) : Converter<Jwt, Collection<GrantedAuthority>> {
   private val vyneRoleDefinitions = vyneUserRoleDefinitionRepository.findAll()
   private val defaultClientUserRoles = vyneUserRoleDefinitionRepository.defaultUserRoles().roles
   private val defaultApiClientUserRoles = vyneUserRoleDefinitionRepository.defaultApiClientUserRoles().roles

   /**
    * Populates set of granted authorities from 'sub' claim of the received jwt token.
    * 'sub' claim value gives us the actual username. From the 'username', first we find the corresponding roles
    * defined for the user and then for each role we fetch the corresponding granted authorities.
    *
    */
   override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
      val preferredUserName = jwt.claims[JwtStandardClaims.PreferredUserName] as String
      val client = jwt.claims[JwtStandardClaims.ClientId]
      if (client != null) {
         // The call came from an api client.
         assignDefaultRoleToApiClient(preferredUserName)
      } else {
         //First check whether this is the first user logged on to the system.
         checkFirstUserLogin(preferredUserName)
      }

      // find all the roles assigned to user.
      val userRoles = vyneUserRoleMappingRepository.findByUserName(preferredUserName)
      if (userRoles == null) {
         // We see the user first the very first time, assign the user to defaultUserRoles.
         logger.info { "A Vyne human user with Preferred User Name $preferredUserName logs in as the first user, assigning ${this.defaultClientUserRoles}" }
         vyneUserRoleMappingRepository.save(preferredUserName, VyneUserRoles(roles = defaultClientUserRoles))

      }
      // map assigned roles to set of granted authorities.
      val userGrantedAuthorities = userRoles?.roles?.flatMap {
         vyneRoleDefinitions[it]?.grantedAuthorities ?: emptySet()
      }?.toSet() ?: emptySet()
      return userGrantedAuthorities.map { grantedAuthority -> SimpleGrantedAuthority(grantedAuthority.constantValue) }
   }

   private fun checkFirstUserLogin(preferredUserName: String) {
      when (vyneUserRoleMappingRepository.size()) {
         0 -> {
            logger.info { "User With Preferred User Name $preferredUserName logs in as the first user, assigning $adminRoleName" }
            vyneUserRoleMappingRepository.save(preferredUserName, VyneUserRoles(roles = setOf(adminRoleName)))
         }

         1 -> {
            // an api client could be the first one to invoke query server.
            val firstUser = vyneUserRoleMappingRepository.findAll().values.first()
            val firstUserName = vyneUserRoleMappingRepository.findAll().keys.first()
            if (firstUserName != preferredUserName && firstUser.type == VyneConsumerType.API.name) {
               // that means the first call to query server came from an api client.
               logger.info { "User With Preferred User Name $preferredUserName logs in as the first user, assigning $adminRoleName" }
               vyneUserRoleMappingRepository.save(preferredUserName, VyneUserRoles(roles = setOf(adminRoleName)))
            }
         }
      }
   }

   /**
    * Invoked only for api client calls, and relies on the fact that Token contains the 'ClientId' claim
    * (TODO this assumption, existence of clientId claim for Client_credentials based flow, is probably only valid for KeyCloack)
    */
   private fun assignDefaultRoleToApiClient(preferredUserName: String) {
      if (vyneUserRoleMappingRepository.findByUserName(preferredUserName) == null) {
         logger.info { "Api Client Preferred User Name $preferredUserName logs in as the first user, assigning ${this.defaultApiClientUserRoles}" }
         vyneUserRoleMappingRepository.save(
            preferredUserName,
            VyneUserRoles(roles = defaultApiClientUserRoles, type = VyneConsumerType.API.name)
         )
      }
   }
}


