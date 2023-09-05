package com.orbitalhq.query.runtime.core.dispatcher.local

import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.query.runtime.core.dispatcher.StreamingQueryDispatcher
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import reactor.core.publisher.Flux


/**
 * Configured by the RoutedQueryDispatcherAdaptor if no query dispatcher was provided.
 *
 * A StreamingQueryDispatcher is responsible for taking requests to saved queries
 * with @Http endpoints, and sending it somewhere to be executed (eg., a serverless function somewhere).
 *
 * If one isn't provided, then we use this, to execute the queries locally.
 * Useful for quick-start projects, but not as scalable as query offloading.
 */
class LocalQueryDispatcher(
   private val queryService: QueryService
) : StreamingQueryDispatcher {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun dispatchQuery(
      query: String,
      clientQueryId: String,
      mediaType: String,
      resultMode: ResultMode,
      arguments: Map<String, Any?>
   ): Flux<Any> {
      // Note: This isn't actually a suspend function.
      // All the work happens in the returned Flux<> / Flow<>,
      // we just need to fix the underling signatures.
      logger.info { "Received inbound call to saved query with request id $clientQueryId. Will execute locally" }
      val responseEntity = runBlocking {
         queryService.submitVyneQlQuery(
            query,
            resultMode,
            mediaType,
            null,
            clientQueryId,
            arguments = arguments
         )
      }
      return responseEntity.body!!
         .asFlux()
   }
}
