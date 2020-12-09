package io.vyne.queryService

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
@ConditionalOnExpression("!\${vyne.query-history.enabled:true}")
class NoopQueryHistory : QueryHistory {
   override fun add(record: QueryHistoryRecord<out Any>) {}
   override fun list(): Flux<QueryHistoryRecord<out Any>> = Flux.empty()
   override fun get(id: String): Mono<QueryHistoryRecord<out Any>> = Mono.empty()
   override fun clear() {}
}
