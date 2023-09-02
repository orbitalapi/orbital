package io.orbital.station

import com.orbitalhq.cockpit.core.FeatureTogglesConfig
import com.orbitalhq.cockpit.core.lsp.LanguageServerConfig
import com.orbitalhq.cockpit.core.pipelines.PipelineConfig
import com.orbitalhq.cockpit.core.security.VyneUserConfig
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.licensing.LicenseConfig
import com.orbitalhq.query.chat.ChatQueryParser
import com.orbitalhq.schemaServer.core.VersionedSourceLoader
import com.orbitalhq.schemaServer.core.config.WorkspaceConfig
import com.orbitalhq.spring.config.DiscoveryClientConfig
import com.orbitalhq.spring.config.VyneSpringCacheConfiguration
import com.orbitalhq.spring.config.VyneSpringHazelcastConfiguration
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.http.auth.HttpAuthConfig
import com.orbitalhq.spring.projection.ApplicationContextProvider
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.util.concurrent.TimeUnit

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
   FeatureTogglesConfig::class,
   WorkspaceConfig::class
)
@Import(
   HttpAuthConfig::class,
   ApplicationContextProvider::class,
   LicenseConfig::class,
   DiscoveryClientConfig::class,
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


   @Bean
   fun chatGptService(@Value("\${vyne.chat-gpt.api-key:''}") apiKey: String): ChatQueryParser {
      return ChatQueryParser(apiKey, OkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS).build())
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

