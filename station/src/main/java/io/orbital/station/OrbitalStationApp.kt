package io.orbital.station

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import com.orbitalhq.cockpit.core.CustomSettings
import com.orbitalhq.cockpit.core.DatabaseConfig
import com.orbitalhq.cockpit.core.FeatureTogglesConfig
import com.orbitalhq.cockpit.core.lsp.LanguageServerConfig
import com.orbitalhq.cockpit.core.security.VyneUserConfig
import com.orbitalhq.copilot.CopilotSpringModule
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.licensing.LicenseConfig
import com.orbitalhq.logging.MDCContextKeys.ClientQueryId
import com.orbitalhq.logging.MDCContextKeys.QueryId
import com.orbitalhq.plugins.PluginLoader
import com.orbitalhq.plugins.PluginLoaderInitializer
import com.orbitalhq.schemaServer.core.VersionedSourceLoader
import com.orbitalhq.schemaServer.core.config.WorkspaceSettings
import com.orbitalhq.spring.config.DiscoveryClientConfig
import com.orbitalhq.spring.config.OrbitalOnly
import com.orbitalhq.spring.config.VyneSpringCacheConfiguration
import com.orbitalhq.spring.config.VyneSpringHazelcastConfiguration
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.http.auth.HttpAuthConfig
import com.orbitalhq.spring.http.websocket.WebSocketPingConfig
import com.orbitalhq.spring.metrics.MicrometerMetricsReporter
import com.orbitalhq.spring.projection.ApplicationContextProvider
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import io.micrometer.context.ContextRegistry
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import reactor.core.publisher.Hooks
import java.nio.file.Paths

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
   CopilotSpringModule::class,
   WebSocketPingConfig::class
)
//@EnableWebFluxSecurity
class OrbitalStationApp {
   private val logger = KotlinLogging.logger {}

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         /**
          * Configuration to pass queryId and ClientQueryId MDC variables across reactor therads.
          * see https://spring.io/blog/2023/03/30/context-propagation-with-project-reactor-3-unified-bridging-between-reactive
          * for more information.
          */
         Hooks.enableAutomaticContextPropagation()

         Hooks.onErrorDropped { e ->
            println("Dropped error: ${e.message}")
            e.printStackTrace()
         }

         Hooks.onOperatorDebug()

         ContextRegistry.getInstance()
            .registerThreadLocalAccessor(ClientQueryId,
               { MDC.get(ClientQueryId) },
               { value -> MDC.put(ClientQueryId, value) },
               { MDC.remove(ClientQueryId) })

         ContextRegistry.getInstance()
            .registerThreadLocalAccessor(
               QueryId,
               { MDC.get(QueryId) },
               { value -> MDC.put(QueryId, value) },
               { MDC.remove(QueryId) })


         // Before starting spring, load any plugins so that scanning is effective
         val pluginLoader: PluginLoader = loadPlugins(args)

         val app = SpringApplication(OrbitalStationApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.setAdditionalProfiles(OrbitalOnly.ORBITAL)
         app.addInitializers(PluginLoaderInitializer(pluginLoader))
         app.run(*args)
      }

      private fun loadPlugins(args: Array<String>): PluginLoader {
         val pluginPathsFromArgs = args.filter { it.startsWith("--vyne.plugins.path") }
            .map { it.split("=")[1] }

         val defaultPluginPaths = listOf(
            Paths.get("plugins.conf").toAbsolutePath().toString(),
            Paths.get(System.getProperty("user.home"), ".orbital/plugins.conf").toAbsolutePath().toString(),
         )

         val pluginLoader = PluginLoader(pluginPathsFromArgs + defaultPluginPaths)
         pluginLoader.loadPlugins()
         return pluginLoader
      }


   }


   @Bean
   fun formatSpecRegistry(): FormatSpecRegistry = FormatSpecRegistry.default()

   @Bean
   fun metricsReporter(meterRegistry: MeterRegistry) = MicrometerMetricsReporter(meterRegistry)


   @Autowired
   fun logInfo(@Autowired(required = false) buildInfo: BuildProperties? = null) {
      val lc: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
      StatusPrinter.print(lc)
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

