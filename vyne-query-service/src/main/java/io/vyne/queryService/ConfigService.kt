package io.vyne.queryService

import io.vyne.queryService.history.db.QueryHistoryConfig
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConfigService(private val config: QueryServerConfig, historyConfig: QueryHistoryConfig) {

   private val configSummary = ConfigSummary(config, historyConfig)

   @GetMapping("/api/config")
   fun getConfig(): ConfigSummary {
      return configSummary
   }
}

// For sending to the UI
data class ConfigSummary(
   val server: QueryServerConfig,
   val history: QueryHistoryConfig
)
