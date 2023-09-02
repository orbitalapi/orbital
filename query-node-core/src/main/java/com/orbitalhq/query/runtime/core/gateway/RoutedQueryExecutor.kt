package com.orbitalhq.query.runtime.core.gateway

import com.orbitalhq.query.runtime.core.dispatcher.StreamingQueryDispatcher
import com.orbitalhq.utils.Ids
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * Responsible for executing queries received from a saved query with an Http()
 * annotation.
 *
 * Will either be executed in-process (eg., by the query server when running in a non-prod config),
 * or offloaded to a QueryFunctionNode by a QueryDispatcher
 */
interface RoutedQueryExecutor {
   fun handleRoutedQuery(query: RoutedQuery): Flux<Any>
}


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
   val dispatcher: StreamingQueryDispatcher?
) : RoutedQueryExecutor {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      if (dispatcher != null) {
         logger.info { "RoutedQueryExecutor created.  Will offload queries to dispatcher of type ${dispatcher!!::class.simpleName}" }
      } else {
         logger.info { "RoutedQueryExecutor created without a dispatcher.  Queries will be rejected." }
      }

   }

   override fun handleRoutedQuery(query: RoutedQuery): Flux<Any> {
      if (dispatcher == null) {
         error("Cannot dispatch a query without a configured streaming consumer.")
      }
      val clientQueryId = Ids.id("routed-query-")
      logger.info { "Received invocation of query ${query.query.name} to route.  Will be routed with queryId $clientQueryId to dispatcher ${dispatcher!!::class.simpleName}" }
      return dispatcher!!.dispatchQuery(
         query.querySrc,
         clientQueryId,
         MediaType.APPLICATION_JSON_VALUE,
         arguments = query.argumentValues
      )
   }

}
