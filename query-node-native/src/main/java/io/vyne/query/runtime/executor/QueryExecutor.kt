package io.vyne.query.runtime.executor

import io.vyne.query.runtime.QueryMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalSerializationApi::class)
@Component
class QueryExecutor(
   private val vyneFactory: StandaloneVyneFactory
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

//   // TODO : Should be part of the serverless executor.
//   fun executeQuery(messageCborWrapper: QueryMessageCborWrapper): CompressedQueryResultWrapper {
//      return try {
//         val message = messageCborWrapper.message()
//         val result = executeQuery(message)
//         val collectedResult = result.collectList().block()
//         CompressedQueryResultWrapper.forResult(collectedResult!!)
//      } catch (e: Exception) {
//         logger.error(e) { "Query execution failed: ${e.message}" }
//         throw e
//      }
//   }

   /**
    * Executes the query.
    *
    * Currently, there's no support for streaming results,
    * so we can't support stream {} queries, or support streaming
    * incremental results out.
    *
    * As a result, large queries may result in OOM.
    */
   fun executeQuery(message: QueryMessage, context: CoroutineContext = EmptyCoroutineContext): Flux<Any> {
      val vyne = vyneFactory.buildVyne(message)
      val args = message.args()
      return runBlocking {
         val resultFlow = vyne.query(
            message.query,
            clientQueryId = message.clientQueryId,
            arguments = args
         )
            .rawResults as Flow<Any>
         resultFlow.asFlux(context)
      }

   }

}
