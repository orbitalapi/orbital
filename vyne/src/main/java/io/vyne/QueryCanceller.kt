package io.vyne

import io.vyne.query.CancelRequestHandler
import io.vyne.query.QueryContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
class QueryCanceller(val queryContext: QueryContext): CancelRequestHandler {
   override fun requestCancel() {
      logger.info { "Requesting Cancel from Query Context with queryId => ${queryContext.queryId}" }
      queryContext.requestCancel()
   }
}
