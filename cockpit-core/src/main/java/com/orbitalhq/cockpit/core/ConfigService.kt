package com.orbitalhq.cockpit.core

import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.http.ServicesConfig
import com.orbitalhq.licensing.License
import com.orbitalhq.licensing.LicenseManager
import com.orbitalhq.plugins.LoadedPlugin
import com.orbitalhq.plugins.PluginLoader
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress
import java.time.Instant


@RestController
class ConfigService(
   analyticsConfig: QueryAnalyticsConfig,
   licenseManager: LicenseManager,
   @Value("\${management.endpoints.web.base-path:/actuator}") actuatorPath: String,
   featureToggles: FeatureTogglesConfig,
   customSettings: CustomSettings,
   pluginLoader: PluginLoader,
   discoveryClient: DiscoveryClient,
) {

   private lateinit var configSummary:ConfigSummary

   init {
      val defaultLicenseServerUrl =
         ServicesConfig.DEFAULT.services[ServicesConfig.LICENSE_SERVER_NAME]?.get(ServicesConfig.URL)!!
      val licenseServer = discoveryClient.getInstances(ServicesConfig.LICENSE_SERVER_NAME)
         .firstOrNull()?.uri?.toASCIIString() ?: defaultLicenseServerUrl

      configSummary =
         ConfigSummary(
            analyticsConfig,
            actuatorPath,
            featureToggles,
            customSettings.custom,
            pluginLoader.loadedPlugins,
            licenseServer
         )
   }

   @GetMapping("/api/config")
   fun getConfig(): ConfigSummary {
      return configSummary
   }
}

// For sending to the UI
data class ConfigSummary(
   val analytics: QueryAnalyticsConfig,
   val actuatorPath: String,
   val featureToggles: FeatureTogglesConfig,
   val custom: Map<String, Any>,
   val loadedPlugins: List<LoadedPlugin>,
   val licenseServerEndpoint: String
)

/**
 * Settings that are provided for bespoke builds, or whitelabel builds.
 */
@ConfigurationProperties(prefix = "vyne.config")
data class CustomSettings(
   val custom: Map<String, Any> = emptyMap()
)

@ConfigurationProperties(prefix = "vyne.toggles")
data class FeatureTogglesConfig(
   val copilotEnabled: Boolean = false,
   val workspacesEnabled: Boolean = false,
   val policiesEnabled: Boolean = false,
   val queryPlanModeEnabled: Boolean = false,
   val serviceLineageDiagramsEnabled: Boolean = false,
   val copyAsCodeEnabled: Boolean = false,
   val nebulaEnabled: Boolean = false,
   val enableLicenseEnforcement: Boolean = false,
)

data class LicenseStatus(
   val isLicensed: Boolean,
   val expiresOn: Instant
) {
   companion object {
      fun from(license: License): LicenseStatus {
         return LicenseStatus(
            isLicensed = !license.isFallbackLicense,
            expiresOn = license.expiresOn
         )
      }
   }
}


@Configuration(proxyBeanMethods = false)
class MyMeterRegistryConfiguration {
   @Bean
   fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
      val hostname = InetAddress.getLocalHost().hostName;
      return MeterRegistryCustomizer { registry: MeterRegistry ->
         registry.config().commonTags(
            "hostname", hostname
         )
      }
   }
}
