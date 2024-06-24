package com.orbitalhq.query.runtime.core.dispatcher.local

import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.query.runtime.core.dispatcher.StreamingQueryDispatcher
import kotlinx.coroutines.runBlocking
import lang.taxi.types.QualifiedName
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


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
   private val queryService: QueryService,
   private val streamResultSubscriptionManager: StreamResultStreamProvider
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
   ): Publisher<Any> {
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
      return when (responseEntity.body) {
         is Flux<*> -> responseEntity.body!! as Flux<Any>
         is Mono<*> -> responseEntity.body!! as Mono<Any>
         else -> error("Unhandled usecase: ${responseEntity.body::class.simpleName}")
      }
   }

   override fun publishResultStream(name: QualifiedName): Flux<Any> {
      return streamResultSubscriptionManager.getResultStream(name.parameterizedName)
   }
}
