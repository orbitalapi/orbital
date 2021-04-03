package io.vyne.queryService.history

import io.vyne.query.history.QueryHistoryRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Useful for testing only.
 * Previously was the default, wheras now we default to local file-based storage
 * using H2.
 */
@Component
@ConditionalOnExpression("\${vyne.query-history.enabled:false}")
class NoopQueryHistory : QueryHistory {
   override fun add(recordProvider: () -> QueryHistoryRecord<out Any>) {}
   override fun list(): Flux<QueryHistoryRecord<out Any>> = Flux.empty()
   override fun get(id: String): Mono<QueryHistoryRecord<out Any>> = Mono.empty()
   override fun clear() {}
}
