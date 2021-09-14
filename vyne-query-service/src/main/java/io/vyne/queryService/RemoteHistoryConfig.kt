package io.vyne.queryService

import io.vyne.history.remote.QueryHistoryRemoteWriter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Activated when Query history is pushed to a remote server for persistence.
 */
@ConditionalOnProperty(prefix = "vyne.history", name = ["mode"], havingValue = "remote", matchIfMissing = false)
@Configuration
class RemoteHistoryConfig {
   @Bean
   fun historyWriterProvider() = QueryHistoryRemoteWriter()
}
