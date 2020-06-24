package io.vyne.queryService.persistency

import io.r2dbc.postgresql.codec.Json
import io.r2dbc.spi.ConnectionFactory
import io.vyne.queryService.QueryHistory
import io.vyne.queryService.QueryHistoryRecord
import io.vyne.queryService.persistency.entity.QueryHistoryRecordEntity
import io.vyne.queryService.persistency.entity.QueryHistoryRecordRepository
import io.vyne.utils.log
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.ArrayList
import java.util.function.BiFunction


class DatabaseBackedQueryHistory(private val repository: QueryHistoryRecordRepository,
                                 private val connectionFactory: ConnectionFactory,
                                 private val queryHistoryRecordReadingConverter: QueryHistoryRecordReadingConverter): QueryHistory {
   override fun add(record: QueryHistoryRecord<out Any>)  {
      repository
         .save(QueryHistoryRecordEntity(queryId = record.id, record = record, timestamp = record.timestamp))
         .subscribeOn(Schedulers.parallel())
         .subscribe { log().info("Persisted query id => ${it.queryId}") }
   }

   override fun list(): Flux<QueryHistoryRecord<out Any>> {
      // ReactiveCrudRepository is really really poor compared to jpa based repos.
      // For instance, Pageable and Sortable query support incomplete and buggy.
      // TODO re-visit below when there are new rdbc2 releases....
      val client = DatabaseClient.builder().connectionFactory(connectionFactory).build()
      return  client.execute("SELECT * FROM query_history_records ORDER BY executed_at DESC LIMIT 10")
         .map { row, _ ->  row.get(2)}
         .all()
         .map {queryHistoryRecordReadingConverter.convert(it as Json)!!}
   }

   override fun get(id: String): Mono<QueryHistoryRecord<out Any>> {
      return repository.findByQueryId(id).map { it.record }
   }
}
