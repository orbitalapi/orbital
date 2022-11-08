package io.orbital.station

import io.orbital.station.lsp.LanguageServerConfig
import io.orbital.station.security.OrbitalUserConfig
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfig
import io.vyne.schemaServer.core.VersionedSourceLoader
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.FileSchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import java.nio.file.Path

@SpringBootApplication(
//   exclude = [SecurityAutoConfiguration::class],
   scanBasePackageClasses = [OrbitalStationApp::class, VersionedSourceLoader::class])
@EnableConfigurationProperties(
   LanguageServerConfig::class,
   OrbitalUserConfig::class
)
//@EnableWebFluxSecurity
class OrbitalStationApp {
   private val logger = KotlinLogging.logger {}
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(OrbitalStationApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }

   @Autowired
   fun logInfo(@Autowired(required = false) buildInfo: BuildProperties? = null) {
      val baseVersion = buildInfo?.get("baseVersion")
      val buildNumber = buildInfo?.get("buildNumber")
      val version = if (!baseVersion.isNullOrEmpty() && buildNumber != "0" && buildInfo.version.contains("SNAPSHOT")) {
         "$baseVersion-BETA-$buildNumber"
      } else {
         buildInfo?.version ?: "Dev version"
      }

      logger.info { "Orbital Station version => $version" }
   }
}


@Configuration
class WebConfig : WebFluxConfigurer {

   @Value("\${orbital.cors.enabled:true}")
   var corsEnabled: Boolean = true

   private val logger = KotlinLogging.logger {}
   override fun addCorsMappings(registry: CorsRegistry) {
      if (!corsEnabled) {
         logger.warn { "CORS is disabled.  Allowing all access" }
         registry
            .addMapping("/**")
      }
   }
}
