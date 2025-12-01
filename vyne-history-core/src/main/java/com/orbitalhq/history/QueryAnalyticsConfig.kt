package com.orbitalhq.history

import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

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

   var persistTraceEvents: Boolean = true,

   // Page size for the historical Query Display in UI.
   val pageSize: Int = 20,

   // Mutable for testing
   // Default is actually ${APP_DATA:orbital_data}/historyPersistenceQueue
   // defined in application.yaml
   // as we want the APP_DATA to be configurable
   var persistenceQueueStorePath: Path = Paths.get("./historyPersistenceQueue"),

   // Mutable for testing
   var persistResults: Boolean = true,

   var persistErrors: Boolean = true,
   // Mutable for testing
   var analyticsServerApplicationName: String = "analytics-server",
   // Mutable for testing
   var mode: AnalyticsMode = AnalyticsMode.Inprocess,

   // Mutable for testing
   var writerMaxBatchSize:Int = 2000,
   var writerMaxDuration:Duration = Duration.ofSeconds(1),

   // The number of results to show in the UI, before result emission
   // is paused client side and a notification is shown to the user
   var maxQueryRecordCount: Int = 5000,
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
