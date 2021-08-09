package io.vyne.queryService

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.queryService.history.db.QueryHistoryConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress


@RestController
class ConfigService(
   private val config: QueryServerConfig,
   historyConfig: QueryHistoryConfig,
   @Value("\${management.endpoints.web.base-path:/actuator}")  actuatorPath: String) {

   private val configSummary = ConfigSummary(config, historyConfig, actuatorPath)

   @GetMapping("/api/config")
   fun getConfig(): ConfigSummary {
      return configSummary
   }
}

// For sending to the UI
data class ConfigSummary(
   val server: QueryServerConfig,
   val history: QueryHistoryConfig,
   val actuatorPath: String
)

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