package io.vyne.query.runtime.core.gateway

import io.vyne.query.runtime.core.dispatcher.StreamingQueryDispatcher
import io.vyne.utils.Ids
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
   val dispatcher: StreamingQueryDispatcher
) : RoutedQueryExecutor {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      logger.info { "RoutedQueryExecutor created.  Will offload queries to dispatcher of type ${dispatcher::class.simpleName}" }
   }

   override fun handleRoutedQuery(query: RoutedQuery): Flux<Any> {
      val clientQueryId = Ids.id("routed-query-")
      logger.info { "Received invocation of query ${query.query.name} to route.  Will be routed with queryId $clientQueryId to dispatcher ${dispatcher::class.simpleName}" }
      return dispatcher.dispatchQuery(
         query.querySrc,
         clientQueryId,
         MediaType.APPLICATION_JSON_VALUE,
         arguments = query.argumentValues
      )
   }

}
