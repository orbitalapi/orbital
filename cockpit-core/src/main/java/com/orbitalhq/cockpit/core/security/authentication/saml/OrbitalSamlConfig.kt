package com.orbitalhq.cockpit.core.security.authentication.saml

import com.orbitalhq.cockpit.core.lsp.LanguageServerConfig
import com.orbitalhq.cockpit.core.security.UserAuthenticatedEventSource
import com.orbitalhq.cockpit.core.security.authorisation.VyneSamlConfig
import mu.KotlinLogging
import org.pac4j.core.config.Config
import org.pac4j.core.matching.matcher.PathMatcher
import org.pac4j.core.profile.factory.ProfileManagerFactory
import org.pac4j.saml.client.SAML2Client
import org.pac4j.saml.config.SAML2Configuration
import org.pac4j.springframework.web.SecurityFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.WebFilter

private val logger = KotlinLogging.logger {  }
@Configuration
@ConditionalOnProperty("vyne.security.saml.enabled", havingValue = "true", matchIfMissing = false)
// to define the callback and logout controllers
@ComponentScan(basePackages = ["org.pac4j.springframework.web"])
class OrbitalSamlConfig {

   @Bean fun userAuthenticatedEventSource() = SamlUserAuthenticatedEventSource()
   @Bean
   fun config(vyneSamlConfig: VyneSamlConfig, userAuthenticatedEventSource: UserAuthenticatedEventSource): Config {
      logger.info { "Configuring with Saml Config with => $vyneSamlConfig" }
      val cfg = SAML2Configuration(
          FileSystemResource(vyneSamlConfig.keyStorePath),
          vyneSamlConfig.keyStorePassword,
          vyneSamlConfig.privateKeyPassword,
          FileSystemResource(vyneSamlConfig.idpMetadataFilePath)
      )
      cfg.maximumAuthenticationLifetime = vyneSamlConfig.maximumAuthenticationLifetime
      cfg.serviceProviderEntityId = vyneSamlConfig.serviceProviderEntityId
      cfg.serviceProviderMetadataResource = FileSystemResource(vyneSamlConfig.serviceProviderMetadataResourcePath)
      cfg.callbackUrl = SamlCallbackUrlProvider.callbackUrl(vyneSamlConfig.callbackBaseUrl)

      val saml2Client = SAML2Client(cfg)
      val config =  Config(cfg.callbackUrl, saml2Client)
      config.profileManagerFactory = ProfileManagerFactory { webContext, sessionStore ->
         SamlProfileManager(webContext, sessionStore, userAuthenticatedEventSource)
      }
      return config
   }

   @Bean
   fun samlFilter(config: Config): WebFilter {
      return SecurityFilter.build(config,
         PathMatcher()
            .excludePath(SamlCallbackUrlProvider.samlCallbackRelativeUrl)
            .excludeBranch("/api/actuator"))
   }
   @Bean
   fun securityWebFilterChain(http: ServerHttpSecurity,
                              languageServerConfig: LanguageServerConfig,
                              @Value("\${management.endpoints.web.base-path:/actuator}") actuatorPath: String): SecurityWebFilterChain? {
       http
         .securityMatcher {
            NegatedServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("/api/security/config", SamlCallbackUrlProvider.samlCallbackUrl)
            )
               .matches(it)
         }
          .csrf(ServerHttpSecurity.CsrfSpec::disable)
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
            "/*.css",

         ).permitAll()
         .anyExchange().authenticated()

      return http.build()
   }
}

object SamlCallbackUrlProvider {
   const val samlCallbackUrl = "saml"
   const val samlCallbackRelativeUrl = "/$samlCallbackUrl"
   fun callbackUrl(callbackBaseUrl: String): String {
      return if (callbackBaseUrl.endsWith("/")) {
         callbackBaseUrl + samlCallbackUrl
      } else {
         callbackBaseUrl + samlCallbackRelativeUrl
      }
   }
}
