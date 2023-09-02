package com.orbitalhq.spring.http.auth

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.auth.schemes.AuthSchemeProvider
import com.orbitalhq.auth.tokens.AuthTokenRepository
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import com.orbitalhq.spring.config.EnvVariablesConfig
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import com.orbitalhq.spring.http.auth.schemes.HoconAuthTokensRepository
import com.orbitalhq.spring.http.auth.schemes.HoconOAuthClientRegistrationRepository
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import java.nio.file.Path
import java.nio.file.Paths

//@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.auth")
data class VyneHttpAuthConfig(
   val configFile: Path = Paths.get("auth.conf")
) {
   companion object {
      val PACKAGE_IDENTIFIER = PackageIdentifier.fromId("com.orbitalhq.config/auth/1.0.0")
   }

}

@Configuration
@EnableConfigurationProperties(VyneHttpAuthConfig::class)
@ComponentScan(basePackageClasses = [AuthWebClientCustomizer::class])
class HttpAuthConfig {

   private val logger = KotlinLogging.logger {}

   @Bean
   fun configAuthTokenRepository(
      config: VyneHttpAuthConfig,
      eventProvider: SchemaChangedEventProvider,
      envVariablesConfig: EnvVariablesConfig
   ): HoconAuthTokensRepository {
      logger.info { "Using auth config file at ${config.configFile.toFile().canonicalPath}" }
      return HoconAuthTokensRepository(
         listOf(
            FileConfigSourceLoader(envVariablesConfig.envVariablesPath, failIfNotFound = false, packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER),
            SchemaConfigSourceLoader(eventProvider, "env.conf"),
            FileConfigSourceLoader(
               config.configFile,
               packageIdentifier = VyneHttpAuthConfig.PACKAGE_IDENTIFIER,
               failIfNotFound = false
            ),
            SchemaConfigSourceLoader(eventProvider, "auth.conf")
         )
      )
   }

   @Bean
   fun oauthClientManager(authSchemeProvider: AuthSchemeProvider): AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager {
      return oauthAuthorizedClientManager(authSchemeProvider)
   }

   @Bean
   @Deprecated("Use configAuthTokenRepository")
   fun authTokenRepository(config: VyneHttpAuthConfig): AuthTokenRepository {
      return if (config.configFile == null) {
         logger.info { "No auth config file has been supplied, so no auth tokens will be injected.  Configure an auth config file by setting auth.conf.config-file." }
         EmptyAuthTokenRepository
      } else {
         logger.info { "Using auth config file at ${config.configFile.toFile().canonicalPath}" }
         ConfigFileAuthTokenRepository(
            config.configFile
         )
      }
   }
}

// exposed to make testing easier
fun oauthAuthorizedClientManager(authSchemeProvider: AuthSchemeProvider): AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager {
   val oAuthClientRegistrationRepository = HoconOAuthClientRegistrationRepository(
      authSchemeProvider
   )
   val authorizedClientService: ReactiveOAuth2AuthorizedClientService =
      InMemoryReactiveOAuth2AuthorizedClientService(oAuthClientRegistrationRepository)

   return AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
      oAuthClientRegistrationRepository,
      authorizedClientService
   )
}
