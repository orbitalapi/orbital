package io.vyne.queryService.security

import io.vyne.queryService.schemas.BuiltInTypesProvider
import io.vyne.queryService.security.access.PrePostAdviceReactiveMethodInterceptor
import io.vyne.queryService.security.authorisation.VyneAuthorisationConfig
import io.vyne.queryService.security.authorisation.VyneUserAuthorisationRole
import io.vyne.queryService.security.authorisation.VyneUserName
import io.vyne.queryService.security.authorisation.VyneUserRoleDefinitionFileRepository
import io.vyne.queryService.security.authorisation.VyneUserRoleDefinitionRepository
import io.vyne.queryService.security.authorisation.VyneUserRoleMappingFileRepository
import io.vyne.queryService.security.authorisation.VyneUserRoleMappingRepository
import io.vyne.queryService.security.authorisation.VyneUserRoles
import io.vyne.schemaPublisherApi.SchemaPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.security.access.expression.method.ExpressionBasedPostInvocationAdvice
import org.springframework.security.access.expression.method.ExpressionBasedPreInvocationAdvice
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.access.method.AbstractMethodSecurityMetadataSource
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.server.ServerWebExchange


@EnableWebFluxSecurity
@EnableConfigurationProperties(VyneAuthorisationConfig::class)
class VyneInSecurityAutoConfig {

   fun getURLsForDisabledCSRF(): NegatedServerWebExchangeMatcher? {
      return NegatedServerWebExchangeMatcher { exchange: ServerWebExchange? -> ServerWebExchangeMatchers.pathMatchers("/eureka/**").matches(exchange) }
   }


   @Bean
   fun vyneUserRepository(config: VyneUserConfig): VyneUserRepository {
      return if (config.configFile == null) {
         // SecureVyneRepository implementation is acting as an empty repository foe the moment.
         SecureVyneRepository()
      } else {
         ConfigFileVyneUserRepository(config.configFile)
      }
   }

   @Bean
   fun vyneUserRoleMappingRepository(vyneAuthorisationConfig: VyneAuthorisationConfig): VyneUserRoleMappingRepository {
      return VyneUserRoleMappingFileRepository(path = vyneAuthorisationConfig.userToRoleMappingsFile)
   }

   @Bean
   fun vyneUserRoleDefinitionRepository(vyneAuthorisationConfig: VyneAuthorisationConfig): VyneUserRoleDefinitionRepository {
      return VyneUserRoleDefinitionFileRepository(path = vyneAuthorisationConfig.roleDefinitionsFile)
   }

   @Bean
   fun onApplicationReadyEventListener(schemaPublisher: SchemaPublisher): ApplicationListener<ApplicationReadyEvent?>? {
      return ApplicationListener { evt: ApplicationReadyEvent? ->
         schemaPublisher.submitSchemas(BuiltInTypesProvider.versionedSources)
      }
   }

   @Profile("!secure")
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

   @Profile("secure")
   @EnableReactiveMethodSecurity
   class VyneReactiveSecurityConfig {
      @Bean
      fun grantedAuthoritiesExtractor(vyneAuthorisationConfig: VyneAuthorisationConfig,
                                      vyneUserRoleMappingRepository: VyneUserRoleMappingRepository,
                                      vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository): GrantedAuthoritiesExtractor {
         return GrantedAuthoritiesExtractor(vyneUserRoleMappingRepository, vyneUserRoleDefinitionRepository, vyneAuthorisationConfig.adminRole)
      }

      @Bean
      fun springWebFilterChain(http: ServerHttpSecurity,
                               @Value("\${management.endpoints.web.base-path:/actuator}")  actuatorPath: String,
                               grantedAuthoritiesExtractor: GrantedAuthoritiesExtractor): SecurityWebFilterChain {
         http
            .csrf().disable()
            //.and()
            //.cors()
            // .and()
            .authorizeExchange()
            // End point for Cask and other vyne based services to fetch the schema in EUREKA schema discovery mode.
            .pathMatchers("/api/security/config", "/api/schemas").permitAll()
            // All other api end points must be protected.
            .pathMatchers("/api/**").authenticated()
            .pathMatchers(
               "/**", // Allow access to any, to support html5 ui routes (eg /types/foo.bar.Baz)
               "/eureka/**",
               "/eureka/",
               "/eureka",
               "/assets/**",
               "/index.html",
               actuatorPath,
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
            .jwtAuthenticationConverter{ jwt ->
               val jwtAuthenticationConverter = JwtAuthenticationConverter()
               jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesExtractor)
               ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter).convert(jwt)
            }
         return http.build()
      }


      @Bean
      fun securityMethodInterceptor(source: AbstractMethodSecurityMetadataSource,
                                    handler: MethodSecurityExpressionHandler): PrePostAdviceReactiveMethodInterceptor {
         val postAdvice = ExpressionBasedPostInvocationAdvice(handler)
         val preAdvice = ExpressionBasedPreInvocationAdvice()
         preAdvice.setExpressionHandler(handler)
         return PrePostAdviceReactiveMethodInterceptor(source, preAdvice, postAdvice)
      }
   }

}


/**
 * Converts given jwt token to set a of granted authorities.
 */
class GrantedAuthoritiesExtractor(
   private val vyneUserRoleMappingRepository: VyneUserRoleMappingRepository,
   vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository,
   private val adminRoleName: VyneUserAuthorisationRole
)
   : Converter<Jwt, Collection<GrantedAuthority>> {
   private val vyneRoleDefinitions = vyneUserRoleDefinitionRepository.findAll()

   /**
    * Populates set of granted authorities from 'sub' claim of the received jwt token.
    * 'sub' claim value gives us the actual user name. From the 'user name', first we find the corresponding roles
    * defined for the user and then for each role we fetch the corresponding granted authorities.
    *
    */
   override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
      val subject = jwt.claims[JwtStandardClaims.Sub] as String
      //First check whether this is the first user logged on to the system.
      checkFirstUserLogin(subject)
      // find all the roles assigned to user.
      val userRoles = vyneUserRoleMappingRepository.findByUserName(subject)
      // map assigned roles to set of granted authorities.
      val userGrantedAuthorities = userRoles?.roles?.flatMap {
         vyneRoleDefinitions[it]?.grantedAuthorities ?: emptySet()
      }?.toSet() ?: emptySet()
      return userGrantedAuthorities.map { grantedAuthority -> SimpleGrantedAuthority(grantedAuthority.constantValue) }
   }

   private fun checkFirstUserLogin(userName: String) {
      if (vyneUserRoleMappingRepository.size() == 0) {
         vyneUserRoleMappingRepository.save(userName, VyneUserRoles(roles = setOf(adminRoleName)))
      }
   }
}


