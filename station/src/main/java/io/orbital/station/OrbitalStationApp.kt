package io.orbital.station

import io.vyne.cockpit.core.lsp.LanguageServerConfig
import io.vyne.cockpit.core.pipelines.PipelineConfig
import io.vyne.cockpit.core.security.VyneUserConfig
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.licensing.LicenseConfig
import io.vyne.schemaServer.core.VersionedSourceLoader
import io.vyne.spring.config.DiscoveryClientConfig
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.HttpAuthConfig
import io.vyne.spring.projection.ApplicationContextProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Import

@SpringBootApplication(
   scanBasePackageClasses = [OrbitalStationApp::class, VersionedSourceLoader::class]
)
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   LanguageServerConfig::class,
   QueryAnalyticsConfig::class,
   PipelineConfig::class,
   VyneSpringProjectionConfiguration::class,
   VyneSpringHazelcastConfiguration::class,
   VyneUserConfig::class,
)
@Import(
   HttpAuthConfig::class,
   ApplicationContextProvider::class,
   LicenseConfig::class,
   DiscoveryClientConfig::class
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

      logger.info { "Orbital Station version $version" }
   }
}

