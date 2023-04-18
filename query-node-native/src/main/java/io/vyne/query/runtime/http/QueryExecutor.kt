package io.vyne.query.runtime.http

import io.vyne.query.runtime.CompressedQueryResultWrapper
import io.vyne.query.runtime.QueryMessage
import io.vyne.query.runtime.QueryMessageCborWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import reactor.core.publisher.Flux
import java.io.InputStream

@OptIn(ExperimentalSerializationApi::class)
@Component
class QueryExecutor(
   private val vyneFactory: StandaloneVyneFactory
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun executeQuery(messageCborWrapper: QueryMessageCborWrapper): CompressedQueryResultWrapper {
      return try {
         val message = messageCborWrapper.message()
         executeQuery(message)
      } catch (e: Exception) {
         logger.error(e) { "Query execution failed: ${e.message}" }
         throw e
      }

   }

   /**
    * Executes the query.
    *
    * Currently, there's no support for streaming results,
    * so we can't support stream {} queries, or support streaming
    * incremental results out.
    *
    * As a result, large queries may result in OOM.
    */
   fun executeQuery(message: QueryMessage): CompressedQueryResultWrapper {
      val vyne = vyneFactory.buildVyne(message)
      val args = message.args()
      val result = runBlocking {
         vyne.query(message.query, clientQueryId = message.clientQueryId, arguments = args)
            .rawResults as Flow<Any>
      }
      val collectedResult = result.asFlux().collectList().block()
      return CompressedQueryResultWrapper.forResult(collectedResult!!)
   }

}
