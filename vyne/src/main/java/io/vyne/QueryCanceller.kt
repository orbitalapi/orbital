package io.vyne

import io.vyne.query.CancelRequestHandler
import io.vyne.query.QueryContext
import kotlinx.coroutines.Job
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
class QueryCanceller(val queryContext: QueryContext, val job: Job): CancelRequestHandler {
   override fun requestCancel() {
      logger.info { "Requesting Cancel from Query Context with queryId => ${queryContext.queryId}" }
      // cancel the parent query job that handles the find { } part.
      job.cancel()
      // this handles cancellation for projection part - see LocalProjectionProvider where we observe cancellation event
      // published by the context.
      queryContext.requestCancel()
   }
}
