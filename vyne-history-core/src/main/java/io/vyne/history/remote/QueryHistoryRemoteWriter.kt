package io.vyne.history.remote

import io.vyne.query.HistoryEventConsumerProvider
import io.vyne.query.QueryEventConsumer

class QueryHistoryRemoteWriter: HistoryEventConsumerProvider {
   override fun createEventConsumer(queryId: String): QueryEventConsumer {
      return RemoteQueryEventConsumerClient()
   }
}
