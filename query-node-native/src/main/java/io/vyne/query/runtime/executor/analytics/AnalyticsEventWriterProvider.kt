package io.vyne.query.runtime.executor.analytics

import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.remote.QueryHistoryRSocketWriter
import io.vyne.models.OperationResult
import io.vyne.query.QueryEvent
import io.vyne.query.QueryEventConsumer
import io.vyne.schemas.Schema
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.stereotype.Component

@Component
class AnalyticsEventWriterProvider(
   private val queryAnalyticsConfig: QueryAnalyticsConfig,
   private val rsocketStrategies: RSocketStrategies
) {


   fun buildAnalyticsEventConsumer(
      queryId: String,
      schema: Schema,
      discoveryClient: DiscoveryClient
   ): QueryEventConsumer? {
      // TODO :  Make this extensible, so we can use things like SNS.
      val writer = QueryHistoryRSocketWriter(
         queryAnalyticsConfig,
         rsocketStrategies, discoveryClient
      )
      val consumer = writer.createEventConsumer(queryId, schema)
      val shutdownDecorator = ShutdownDecorator(consumer) {
         writer.shutDown()
      }
      return shutdownDecorator
   }
}

class ShutdownDecorator(private val delegate: QueryEventConsumer, val onShutdown: () -> Unit) : QueryEventConsumer {
   override fun handleEvent(event: QueryEvent) = delegate.handleEvent(event)

   override fun recordResult(operation: OperationResult, queryId: String) = delegate.recordResult(operation, queryId)

   override fun shutdown() = onShutdown()

}
