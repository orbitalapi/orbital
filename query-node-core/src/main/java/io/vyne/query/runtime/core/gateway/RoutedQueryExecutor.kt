package io.vyne.query.runtime.core.gateway

import reactor.core.publisher.Mono

/**
 * Responsible for executing queries received from a saved query with an Http()
 * annotation.
 *
 * Will either be executed in-process (eg., by the query server when running in a non-prod config),
 * or offloaded to a QueryFunctionNode by a QueryDispatcher
 */
interface RoutedQueryExecutor {
   fun  handleRoutedQuery(query: RoutedQuery): Mono<Any>
}
