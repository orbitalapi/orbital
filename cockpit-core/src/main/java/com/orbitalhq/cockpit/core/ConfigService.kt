package com.orbitalhq.cockpit.core

import com.orbitalhq.cockpit.core.pipelines.PipelineConfig
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.licensing.License
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress
import java.time.Instant


@RestController
class ConfigService(
   analyticsConfig: QueryAnalyticsConfig,
   pipelineConfig: PipelineConfig,
   license: License,
   @Value("\${management.endpoints.web.base-path:/actuator}") actuatorPath: String,
   val featureToggles: FeatureTogglesConfig,
   val customSettings: CustomSettings
) {

   private val configSummary =
      ConfigSummary(
         analyticsConfig,
         pipelineConfig,
         LicenseStatus.from(license),
         actuatorPath,
         featureToggles,
         customSettings.custom

      )

   @GetMapping("/api/config")
   fun getConfig(): ConfigSummary {
      return configSummary
   }
}

// For sending to the UI
data class ConfigSummary(
   val analytics: QueryAnalyticsConfig,
   val pipelineConfig: PipelineConfig,
   val licenseStatus: LicenseStatus,
   val actuatorPath: String,
   val featureToggles: FeatureTogglesConfig,
   val custom: Map<String,Any>
)

/**
 * Settings that are provided for bespoke builds, or whitelabel builds.
 */
@ConfigurationProperties(prefix="vyne.config")
data class CustomSettings(
   val custom: Map<String,Any> = emptyMap()
)

@ConfigurationProperties(prefix = "vyne.toggles")
data class FeatureTogglesConfig(
   val chatGptEnabled: Boolean = false,
   val workspacesEnabled: Boolean = false,
   val onboardingEnabled: Boolean = false
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
