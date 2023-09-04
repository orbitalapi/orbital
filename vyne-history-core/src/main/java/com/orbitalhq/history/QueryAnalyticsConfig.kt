package com.orbitalhq.history

import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties(prefix = "vyne.analytics")
data class QueryAnalyticsConfig(
   /**
    * Defines the max payload size to persist.
    * Set to 0 to disable persisting the body of responses
    */
   val maxPayloadSizeInBytes: Int = 2048,
   // Mutable for testing
   var persistRemoteCallResponses: Boolean = true,

   var persistRemoteCallMetadata: Boolean = true,
   // Page size for the historical Query Display in UI.
   val pageSize: Int = 20,

   // Mutable for testing
   var persistenceQueueStorePath: Path = Paths.get("./historyPersistenceQueue"),

   // Mutable for testing
   var persistResults: Boolean = true,
   // Mutable for testing
   var analyticsServerApplicationName: String = "analytics-server",
   // Mutable for testing
   var mode: AnalyticsMode = AnalyticsMode.Inprocess
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   init {
       logger.info { "Running with Analytics config: $this" }
   }
}

enum class AnalyticsMode {
   Inprocess,
   Remote,
   None
}
