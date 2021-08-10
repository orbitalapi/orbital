package io.vyne.spring.http.auth

import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.nio.file.Paths

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.auth")
data class VyneHttpAuthConfig(
   val configFile: Path = Paths.get("auth.conf")
)

@Configuration
@EnableConfigurationProperties(VyneHttpAuthConfig::class)
class HttpAuthConfig {

   private val logger = KotlinLogging.logger {}

   @Bean
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
