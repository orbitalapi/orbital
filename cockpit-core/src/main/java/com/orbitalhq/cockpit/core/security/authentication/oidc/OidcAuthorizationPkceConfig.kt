package com.orbitalhq.cockpit.core.security.authentication.oidc

import com.orbitalhq.auth.CookieOrHeaderTokenConverter
import com.orbitalhq.auth.authorisation.VyneUserRoleDefinitionRepository
import com.orbitalhq.cockpit.core.lsp.LanguageServerConfig
import com.orbitalhq.cockpit.core.security.FrontEndSecurityConfig
import com.orbitalhq.cockpit.core.security.authorisation.JwtRolesExtractor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Sets up config for supporting authn / authz via OIDC Authorization Code with PKCE flow.
 *
 * In this flow:
 *  * User hits our SPA, which is loaded, and checks for auth token presence
 *  * If no auth token is provided, they are redirected to the OIDC server to authenticate
 *  * They're redirected to our SPA, which calls the /token endpoint on the OIDC server, which serves JWT's for the now authenticated user
 *  * The JWT is sent to our server as an authorization header
 *  * Our server downloads the signing keys from the OIDC server, and validates the JWT
 *
 */
@ConditionalOnProperty("vyne.security.openIdp.enabled", havingValue = "true", matchIfMissing = false)
@Configuration
// useAuthorizationManager = false required for support for method based PreAuthorize when returning a Kotlin Flow
// see: https://github.com/spring-projects/spring-security/issues/12821
@EnableReactiveMethodSecurity(useAuthorizationManager = false)
class OidcAuthorizationPkceConfig {
   @Bean
   fun grantedAuthoritiesExtractor(
      rolesExtractor: JwtRolesExtractor,
      vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository
   ): GrantedAuthoritiesExtractor {
      return GrantedAuthoritiesExtractor(
         vyneUserRoleDefinitionRepository,
         rolesExtractor
      )
   }

   @Bean
   fun springWebFilterChain(
      http: ServerHttpSecurity,
      languageServerConfig: LanguageServerConfig,
      @Value("\${management.endpoints.web.base-path:/actuator}") actuatorPath: String,
      grantedAuthoritiesExtractor: GrantedAuthoritiesExtractor,
      oidcConfig: FrontEndSecurityConfig
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
         .pathMatchers("/api/actuator/**").permitAll()
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
         .oauth2ResourceServer { spec ->
            spec.jwt { jwtSpec ->
jwtSpec.jwkSetUri(oidcConfig.jwksUri)
            }
         }
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
