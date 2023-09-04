package com.orbitalhq.query.runtime.executor.analytics

import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.remote.QueryHistoryRSocketWriter
import com.orbitalhq.models.OperationResult
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.schemas.Schema
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
   ): QueryEventConsumer {
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
