package io.vyne.history

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path
import java.nio.file.Paths

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.analytics")
data class QueryAnalyticsConfig(
   /**
    * Defines the max payload size to persist.
    * Set to 0 to disable persisting the body of responses
    */
   val maxPayloadSizeInBytes: Int = 2048,
   // Mutable for testing
   var persistRemoteCallResponses: Boolean = true,
   // Page size for the historical Query Display in UI.
   val pageSize: Int = 20,

   // Mutable for testing
   var persistenceQueueStorePath: Path = Paths.get("./historyPersistenceQueue"),

   // Mutable for testing
   var persistResults: Boolean = true,
   // Mutable for testing
   var analyticsServerApplicationName: String = "VYNE-ANALYTICS-SERVER",
   // Mutable for testing
   var mode: AnalyticsMode = AnalyticsMode.Inprocess
)

enum class AnalyticsMode {
   Inprocess,
   Remote
}
