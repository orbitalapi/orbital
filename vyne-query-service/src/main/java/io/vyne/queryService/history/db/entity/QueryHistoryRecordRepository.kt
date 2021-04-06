package io.vyne.queryService.history.db.entity

import org.springframework.data.jpa.repository.JpaRepository

interface QueryHistoryRecordRepository : JpaRepository<PersistentQuerySummary, String> {
//   fun findByQueryId(queryId: String): Mono<PersistentQuerySummary>
//   fun findByIdNotNull(page: Pageable): Flux<PersistentQuerySummary>
}

interface QueryResultRowRepository : JpaRepository<QueryResultRow, Long> {

}

interface LineageRecordRepository : JpaRepository<LineageRecord, String>
