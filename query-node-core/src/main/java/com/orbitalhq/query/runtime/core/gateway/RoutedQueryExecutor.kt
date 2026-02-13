package com.orbitalhq.query.runtime.core.gateway

import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.query.runtime.core.dispatcher.StreamingQueryDispatcher
import com.orbitalhq.query.runtime.core.dispatcher.local.LocalQueryDispatcher
import com.orbitalhq.schemas.QueryCompiler
import com.orbitalhq.utils.Ids
import lang.taxi.query.QueryMode
import mu.KotlinLogging
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.security.Principal

/**
 * Responsible for executing queries received from a saved query with an Http()
 * annotation.
 *
 * Will either be executed in-process (eg., by the query server when running in a non-prod config),
 * or offloaded to a QueryFunctionNode by a QueryDispatcher
 */
interface RoutedQueryExecutor {

   // Routed queries don't currently support streaming.
   // See notes on StreamingQueryDispatcher for considerations when implementing
   fun handleRoutedQuery(query: RoutedQuery, principal: Principal?): RoutedQueryResponse
}

data class RoutedQueryResponse(val publisher: Publisher<Any>, val responseHeaders: Map<String, List<String>>)


/**
 * Very simple facade that takes a RoutedQuery, and hands it off
 * to a dispatcher.
 *
 * Probably don't need any implementations other than this, but
 * if we do, this one will back off, and allow the other implementation to
 * take precedence.
 */
//@ConditionalOnMissingBean(RoutedQueryExecutor::class)
//@ConditionalOnBean(StreamingQueryDispatcher::class)
@Component
class RoutedQueryDispatcherAdaptor(
   // We can't use conditional wiring in Graal native images.
   // So, this needs to be nullable, and we need to handle the scenario that it wasn't wired.
   configuredDispatcher: StreamingQueryDispatcher?,
   val queryService: QueryService,
   val streamResultSubscriptionManager: StreamResultStreamProvider
) : RoutedQueryExecutor {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val dispatcher:StreamingQueryDispatcher = if (configuredDispatcher != null) {
      logger.info { "RoutedQueryExecutor created.  Will offload queries to dispatcher of type ${configuredDispatcher::class.simpleName}" }
      configuredDispatcher
   } else {
      logger.info { "RoutedQueryExecutor created without a dispatcher.  Queries will be executed locally." }
      LocalQueryDispatcher(queryService, streamResultSubscriptionManager)
   }


   override fun handleRoutedQuery(query: RoutedQuery, principal: Principal?): RoutedQueryResponse {
      logger.info { "Received invocation of query ${query.query.name} to route.  Will be routed with clientQueryId ${query.clientQueryId} to dispatcher ${dispatcher!!::class.simpleName}" }
      return if (query.query.queryMode == QueryMode.STREAM) {
         RoutedQueryResponse(dispatcher.publishResultStream(
            query.query.name,principal
         ), emptyMap())
      } else {
         dispatcher.dispatchQuery(
            QueryCompiler.asCompiledQueryReference(query.query),
            query.clientQueryId,
            MediaType.APPLICATION_JSON_VALUE,
            arguments = query.argumentValues,
            principal = principal
         )
      }

   }

}
