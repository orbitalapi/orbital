package io.vyne.query.runtime.executor.serverless

import io.vyne.query.runtime.CompressedQueryResultWrapper
import io.vyne.query.runtime.QueryMessageCborWrapper
import io.vyne.query.runtime.executor.QueryExecutor
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class ServerlessQueryExecutor(
   private val executor: QueryExecutor
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun executeQuery(messageCborWrapper: QueryMessageCborWrapper): CompressedQueryResultWrapper {
      return try {
         val message = messageCborWrapper.message()
         val result = executor.executeQuery(message)
         val collectedResult = result.collectList().block()
         CompressedQueryResultWrapper.forResult(collectedResult!!)
      } catch (e: Exception) {
         logger.error(e) { "Query execution failed: ${e.message}" }
         throw e
      }
   }
}
