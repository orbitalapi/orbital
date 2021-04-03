package io.vyne.queryService.history.db.entity

import org.springframework.data.domain.Pageable

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface QueryHistoryRecordRepository: ReactiveCrudRepository<QueryHistoryRecordEntity, Long> {
   fun findByQueryId(queryId: String): Mono<QueryHistoryRecordEntity>
   fun findByIdNotNull(page: Pageable): Flux<QueryHistoryRecordEntity>
}
