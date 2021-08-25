package io.vyne.history.remote

import io.vyne.models.OperationResult
import io.vyne.query.QueryEvent
import io.vyne.query.QueryEventConsumer
import io.vyne.query.RemoteCallOperationResultHandler

class RemoteQueryEventConsumerClient: QueryEventConsumer, RemoteCallOperationResultHandler {
   override fun handleEvent(event: QueryEvent) {

   }

   override fun recordResult(operation: OperationResult, queryId: String) {

   }
}
