package io.vyne.query.runtime.executor.serverless

import io.vyne.query.runtime.CompressedQueryResultWrapper
import io.vyne.query.runtime.QueryMessageCborWrapper
import io.vyne.query.runtime.executor.rabbitmq.RabbitMqQueryExecutor
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * This is a query executor that accepts invocations
 * over Serverless (AWS Lambda), but writes responses
 * over RabbitMQ.
 *
 * This works around message size limits on AWS Lambda.
 */
@Component
class ServerlessOverRabbitQueryExecutor(
   private val rabbitMqQueryExecutor: RabbitMqQueryExecutor?
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun executeQuery(messageWrapper: QueryMessageCborWrapper): CompressedQueryResultWrapper {
      require(rabbitMqQueryExecutor != null) { "RabbitMQ executor was not provided.  Ensure that it has been enabled" }
      return try {
         logger.info { "Received new query over serverless wrapper" }
         val message = messageWrapper.message()
         logger.info { "Query ${message.clientQueryId} successfully decoded, routing for execution" }
         rabbitMqQueryExecutor.executeQueryAndWriteResponsesToRabbit(message)
            .collectList()
            .block()
         logger.info { "Query execution has completed and results have been written to RabbitMQ. Returning placeholder message to serverless" }
         CompressedQueryResultWrapper.forResult(mapOf("message" to "Result written to RabbitMQ"))
      } catch (e: Exception) {
         logger.error(e) { "Query execution failed: ${e.message}" }
         throw e
      }
   }
}
