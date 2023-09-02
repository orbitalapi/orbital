package com.orbitalhq.history.noop

import com.orbitalhq.models.OperationResult
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.schemas.Schema
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@ConditionalOnProperty(prefix = "vyne.analytics", name = ["mode"], havingValue = "None", matchIfMissing = false)
@Configuration
class NoopQueryEventConsumerConfiguration {

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   @Bean
   fun historyWriterProvider():NoopQueryEventConsumer {
      logger.info { "Analytics data is not being stored" }
      return NoopQueryEventConsumer
   }
}

object NoopQueryEventConsumer : HistoryEventConsumerProvider, QueryEventConsumer {
   override fun createEventConsumer(queryId: String, schema: Schema): QueryEventConsumer {
      return this
   }

   override fun handleEvent(event: QueryEvent) {
   }

   override fun recordResult(operation: OperationResult, queryId: String) {
   }

}
