package io.vyne.queryService

import io.vyne.queryService.history.db.QueryHistoryConfig
import io.vyne.queryService.pipelines.PipelineConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConfigService(
   private val config: QueryServerConfig,
   historyConfig: QueryHistoryConfig,
   pipelineConfig: PipelineConfig,
   @Value("\${management.endpoints.web.base-path:/actuator}")  actuatorPath: String) {

   private val configSummary = ConfigSummary(config, historyConfig, pipelineConfig, actuatorPath)

   @GetMapping("/api/config")
   fun getConfig(): ConfigSummary {
      return configSummary
   }
}

// For sending to the UI
data class ConfigSummary(
   val server: QueryServerConfig,
   val history: QueryHistoryConfig,
   val pipelineConfig: PipelineConfig,
   val actuatorPath: String
)
