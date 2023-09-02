package com.orbitalhq.query.runtime.executor.serverless

import com.orbitalhq.query.runtime.CompressedQueryResultWrapper
import com.orbitalhq.query.runtime.QueryMessageCborWrapper
import com.orbitalhq.query.runtime.executor.QueryExecutor
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
         val message = "Query execution failed: ${e.message}"
         logger.error(e) { message }
         CompressedQueryResultWrapper.forError(message)
      }
   }
}
