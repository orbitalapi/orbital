package io.orbital.station

import com.orbitalhq.cockpit.core.CustomSettings
import com.orbitalhq.cockpit.core.DatabaseConfig
import com.orbitalhq.cockpit.core.FeatureTogglesConfig
import com.orbitalhq.cockpit.core.lsp.LanguageServerConfig
import com.orbitalhq.cockpit.core.security.VyneUserConfig
import com.orbitalhq.copilot.CopilotSpringModule
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.licensing.LicenseConfig
import com.orbitalhq.schemaServer.core.VersionedSourceLoader
import com.orbitalhq.schemaServer.core.config.WorkspaceSettings
import com.orbitalhq.spring.config.DiscoveryClientConfig
import com.orbitalhq.spring.config.OrbitalOnly
import com.orbitalhq.spring.config.VyneSpringCacheConfiguration
import com.orbitalhq.spring.config.VyneSpringHazelcastConfiguration
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.http.auth.HttpAuthConfig
import com.orbitalhq.spring.metrics.MicrometerMetricsReporter
import com.orbitalhq.spring.projection.ApplicationContextProvider
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootApplication(
   scanBasePackageClasses = [OrbitalStationApp::class, VersionedSourceLoader::class],
   exclude = [MongoReactiveAutoConfiguration::class]

)
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   LanguageServerConfig::class,
   QueryAnalyticsConfig::class,
   VyneSpringProjectionConfiguration::class,
   VyneSpringHazelcastConfiguration::class,
   VyneUserConfig::class,
   FeatureTogglesConfig::class,
   CustomSettings::class,
   WorkspaceSettings::class,
   DatabaseConfig::class,

)
@Import(
   HttpAuthConfig::class,
   ApplicationContextProvider::class,
   LicenseConfig::class,
   DiscoveryClientConfig::class,
   CopilotSpringModule::class
)
//@EnableWebFluxSecurity
class OrbitalStationApp {
   private val logger = KotlinLogging.logger {}

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(OrbitalStationApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.setAdditionalProfiles(OrbitalOnly.ORBITAL)
         app.run(*args)
      }
   }


   @Bean
   fun formatSpecRegistry(): FormatSpecRegistry = FormatSpecRegistry.default()

   @Bean
   fun metricsReporter(meterRegistry: MeterRegistry) = MicrometerMetricsReporter(meterRegistry)


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

