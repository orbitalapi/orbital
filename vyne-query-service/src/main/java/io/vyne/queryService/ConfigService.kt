package io.vyne.queryService

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.licensing.License
import io.vyne.queryService.pipelines.PipelineConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress
import java.time.Instant


@RestController
class ConfigService(
   private val config: QueryServerConfig,
   analyticsConfig: QueryAnalyticsConfig,
   pipelineConfig: PipelineConfig,
   license: License,
   @Value("\${management.endpoints.web.base-path:/actuator}") actuatorPath: String
) {

   private val configSummary =
      ConfigSummary(config, analyticsConfig, pipelineConfig, LicenseStatus.from(license), actuatorPath)

   @GetMapping("/api/config")
   fun getConfig(): ConfigSummary {
      return configSummary
   }
}

// For sending to the UI
data class ConfigSummary(
   val server: QueryServerConfig,
   val analytics: QueryAnalyticsConfig,
   val pipelineConfig: PipelineConfig,
   val licenseStatus: LicenseStatus,
   val actuatorPath: String
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
